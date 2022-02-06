package com.unipi.channelsio;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Base64;

/**
 * Classe che permette l invio dei dati su uno o più canali.<br>
 * E' garantito che tutti i dati inviati mediante il metodo {@link #sendLine(String)} vengano ricevuti,
 * in ordine di invio, dalle corrispettive chiamate del metodo {@link ChannelReceiver#receiveLine()}<br>
 * <p><br>
 * Es. {@link ChannelReceiver} vs {@link SocketChannel}<br>
 * ChannelSender.sendLine("ciao");<br>
 * ChannelSender.sendLine("mondo");<br>
 * <p><br>
 * ChannelReceiver.readLine(); -> restituirà "ciao"<br>
 * ChannelReceiver.readLine(); -> restituirà "mondo"<br>
 * <p>===========================================================<br>
 * SocketChannel.write("ciao");<br>
 * SocketChannel.write("mondo");<br>
 * <p><br>
 * SocketChannel.read(); -> può restituire "ciao", "ciao mon", "ciao mondo" ecc<br>
 * SocketChannel.read(); -> può generare errori<br>
 */
public class ChannelSender implements Sender {
    private SocketChannel socket;
    private ByteBuffer buffer;

    public ChannelSender(SocketChannel socket) {
        this.socket = socket;

        buffer = ByteBuffer.allocateDirect(128);
    }

    public ChannelSender() {
        this(null);
    }

    /**
     * Invia del testo sul canale settato mediante {@link #setChannel(SocketChannel)} o su quello passato al costruttore.<br>
     * E' garantito che msg venga inviato tutto.<br>
     *
     * @param msg il messaggio da inviare
     * @throws IOException se avviene qualche errore di I/O
     */
    public void sendLine(String msg) throws IOException {
        if (socket == null) return;

        String message = msg;
        if (!msg.endsWith(System.lineSeparator()))
            message = msg.concat(System.lineSeparator());

        int len = buffer.capacity() + 1;
        if (buffer.capacity() > msg.length())
            len = message.length();

        for (int i = 0; i < message.length(); ) {
            buffer.put(message.getBytes(), i, Math.min(buffer.capacity(), len));
            buffer.flip();
            i += socket.write(buffer);

            len = message.length() - i;
            buffer.clear();
        }

        buffer.clear();
    }

    /**
     * Metodo per cambiare canale di invio.
     * Successive chiamate a {@link #sendLine(String)}, vengono eseguite su channel.
     *
     * @param channel il canale su cui questo Sender deve ascoltare o null se si vuole che questo canale non mai invii nulla
     */
    public void setChannel(SocketChannel channel) {
        this.socket = channel;
    }

    /**
     * Metodo per inviare un intero sul canale settato da {@link #setChannel(SocketChannel)}.<br>
     * L intero viene prima convertito in stringa e poi inviato mediante {@link #sendLine(String)}.<br>
     * E' possibile usare il metodo {@link ChannelReceiver#receiveInteger()} per ricevere il dato come intero
     * o il metodo {@link ChannelReceiver#receiveLine()} per riceverlo come stringa.<br>
     *
     * @param i il numero da inviare
     * @throws IOException se si verifica qualche errore di I/O
     */
    public void sendInteger(int i) throws IOException {
        if (socket == null) return;

        sendLine(String.valueOf(i));
    }

    /**
     * Invia un oggetto sul canale settato usando {@link #setChannel(SocketChannel)}.<br>
     * L oggetto viene serializzato, convertito in stringa e inviato usando
     * il metodo {@link #sendLine(String)}
     *
     * @param obj l oggetto che si vuole inviare
     * @throws IOException Se si verifica qualche errore di I/O
     */
    public void sendObject(Object obj) throws IOException {
        if (socket == null) return;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(out));
        oos.writeObject(obj);
        oos.close();

        String serial = Base64.getEncoder().encodeToString(out.toByteArray());
        sendLine(serial);
    }

    /**
     * Metodo per silenziare questo {@link Sender}.<br>
     * Una chiamata a questo metodo ha lo stesso effetto di: <br>
     * setChannel(null)
     * <p>
     * Successive chiamate a qualunque metodo send, non produrranno errori.
     */
    public void setSleepy() {
        setChannel(null);
    }
}
