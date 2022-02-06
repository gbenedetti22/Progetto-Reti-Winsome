package com.unipi.channelsio.concurrent;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentReceiverBuilder {
    private static final ConcurrentHashMap<SocketChannel, String> chunksMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<SocketChannel, ReentrantLock> locksMap = new ConcurrentHashMap<>();

    public static ConcurrentChannelReceiver newConcurrentReceiver(){
        return new ConcurrentChannelReceiver(chunksMap, locksMap);
    }
}
