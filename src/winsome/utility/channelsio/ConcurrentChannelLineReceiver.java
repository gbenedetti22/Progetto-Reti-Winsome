package winsome.utility.channelsio;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Versione concorrente di {@link ChannelLineReceiver}<br>
 * <br>
 * Questa classe permette l utilizzo di più ChannelSender su più thread, cosa non possibile nella versione normale.<br>
 * L utilizzo di questa classe comporta il benificio di avere più thread in ascolto sul solito canale e di poter
 * avere un {@link ChannelReceiver} per ogni thread.
 * <br>
 * Per costruire un nuovo ConcurrentChannelLineReceiver usare il metodo:<br>
 * {@link ChannelReceiver#newConcurrentReceiver()}<br>
 * <br>
 * Questa classe è null-safe.
 */
public class ConcurrentChannelLineReceiver implements ChannelReceiver {
    private final String SEPARATOR = System.lineSeparator();
    private SocketChannel channel;
    private StringBuilder builder;
    private ConcurrentHashMap<SocketChannel, String> chunks;
    private ConcurrentHashMap<SocketChannel, ReentrantLock> locks;
    private ByteBuffer buffer;

    protected ConcurrentChannelLineReceiver(ConcurrentHashMap<SocketChannel, String> chunks, ConcurrentHashMap<SocketChannel, ReentrantLock> locks) {
        this.chunks = chunks;
        this.locks = locks;

        builder = new StringBuilder();
        buffer = ByteBuffer.allocate(512);
    }

    /**
     * Vedi {@link ChannelLineReceiver#receiveLine()}<br>
     * <br>
     *
     * @return la stringa inviata mediante {@link ChannelLineSender#sendLine(String)} o null se EOF viene raggiunto
     * @throws IOException se avviene un errore di I/O
     */
    public String receiveLine() throws IOException {
        if (channel == null || !channel.isOpen()) return "";

        locks.get(channel).lock();

        builder.setLength(0);

        String chunk = chunks.get(channel);
        if (chunk != null) {
            builder.append(chunk);
            chunks.remove(channel);
        }

        int nBytes = 0;
        int offset = 0;

        while (builder.indexOf(SEPARATOR, (offset - nBytes) - SEPARATOR.length()) == -1) {
            try {
                nBytes = channel.read(buffer);
            } catch (ClosedChannelException e) {
                ReentrantLock lock = locks.remove(channel);
                if (lock != null)
                    lock.unlock();

                return null;
            } catch (SocketException e) {
                locks.get(channel).unlock();
                return null;
            }

            if (nBytes == -1) {
                buffer.clear();
                chunks.remove(channel);
                locks.get(channel).unlock();
                return null;
            }

            builder.append(new String(buffer.array(), 0, nBytes));

            offset += nBytes;

            buffer.clear();
        }

        String message = builder.toString();
        String[] data = message.split(System.lineSeparator(), 2);
        message = data[0];

        if (data.length > 1 && !data[1].isBlank()) {
            String remeining = data[1];

            chunks.put(channel, remeining);
        }

        buffer.clear();

        locks.get(channel).unlock();
        return message;
    }

    /**
     * Vedi {@link ChannelLineReceiver#receiveInteger()}<br>
     * <br>
     *
     * @return l intero inviato da {@link ChannelLineSender#sendInteger(int)}
     * @throws IOException           se avviene un errore di I/O
     * @throws NumberFormatException se l intero ricevuto non è un intero
     */
    public int receiveInteger() throws IOException, NumberFormatException {
        if (channel == null) return -1;

        String s = receiveLine();
        if (s == null) {
            throw new EOFException("End of stream reached");
        }

        int i;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Impossibile convertire in numero: " + s);
        }
        return i;
    }

    /**
     * Vedi {@link ChannelLineReceiver#receiveObject()}<br>
     * <br>
     *
     * @return l oggetto inviato da {@link ChannelLineSender#sendObject(Object)}
     * @throws IOException se avviene un errore di I/O
     */
    public Object receiveObject() throws IOException {
        if (channel == null) return null;

        locks.get(channel).lock();

        try {
            String serial = receiveLine();
            if (serial == null) return null;

            byte[] receivedSerial = Base64.getDecoder().decode(serial.getBytes());
            ByteArrayInputStream in = new ByteArrayInputStream(receivedSerial);
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(in));
            Object obj = ois.readObject();
            ois.close();

            return obj;
        } catch (EOFException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            locks.get(channel).unlock();
        }
    }

    /**
     * Questo metodo permette di cambiare canale d ascolto <br>
     * <br>
     *
     * @param channel il canale su cui ascoltare
     */
    public void setChannel(SocketChannel channel) {
        this.channel = channel;
        if (channel != null)
            locks.putIfAbsent(channel, new ReentrantLock());
    }


    /**
     * Questo metodo permette di ottenere il canale su cui questo {@link ChannelReceiver} sta ricevendo i dati
     *
     * @return il canale d ascolto
     */
    public SocketChannel getListeningChannel() {
        return channel;
    }

    @Override
    public String receive() throws IOException {
        return receiveLine();
    }
}
