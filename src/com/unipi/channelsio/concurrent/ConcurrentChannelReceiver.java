package com.unipi.channelsio.concurrent;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
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
        if (channel == null || !channel.isOpen()) return null;

        locks.putIfAbsent(channel, new ReentrantLock());
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
            }

            if (nBytes < 0) {
                buffer.clear();
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

    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }


    public SocketChannel getListeningChannel() {
        return channel;
    }
}
