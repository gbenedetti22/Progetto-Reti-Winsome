package com.unipi.utility.channelsio.concurrent;

import java.io.*;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentChannelReceiver {
    private SocketChannel channel;
    private StringBuilder builder;
    private ConcurrentHashMap<SocketChannel, String> chunks;
    private ConcurrentHashMap<SocketChannel, ReentrantLock> locks;
    private final String SEPARATOR = System.lineSeparator();
    private ByteBuffer buffer;

    protected ConcurrentChannelReceiver(ConcurrentHashMap<SocketChannel, String> chunks, ConcurrentHashMap<SocketChannel, ReentrantLock> locks) {
        this.chunks = chunks;
        this.locks = locks;

        builder = new StringBuilder();
        buffer = ByteBuffer.allocate(10);
    }

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
            }catch (ClosedChannelException e){
                ReentrantLock lock = locks.remove(channel);
                if(lock != null)
                    lock.unlock();

                return null;
            }catch (SocketException e){
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

    public int receiveInteger() throws IOException, NumberFormatException {
        if (channel == null) return -1;

        String s = receiveLine();
        if (s == null){
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
        }finally {
            locks.get(channel).unlock();
        }
    }

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
        if(channel != null)
            locks.putIfAbsent(channel, new ReentrantLock());
    }


    public SocketChannel getListeningChannel() {
        return channel;
    }
}
