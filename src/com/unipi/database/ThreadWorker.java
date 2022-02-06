package com.unipi.database;

import com.unipi.channelsio.ChannelSender;
import com.unipi.channelsio.concurrent.ConcurrentChannelReceiver;
import com.unipi.channelsio.concurrent.ConcurrentReceiverBuilder;

import java.util.concurrent.ThreadFactory;

public class ThreadWorker extends Thread {
    private static class WorkerFactory implements ThreadFactory{

        @Override
        public Thread newThread(Runnable r) {
            return new ThreadWorker(r);
        }
    }

    private ConcurrentChannelReceiver receiver;
    private ChannelSender sender;

    public ThreadWorker(Runnable target) {
        super(target);

        receiver = ConcurrentReceiverBuilder.newConcurrentReceiver();
        sender = new ChannelSender();
    }

    public ThreadWorker(){
        receiver = ConcurrentReceiverBuilder.newConcurrentReceiver();
        sender = new ChannelSender();
    }

    public ConcurrentChannelReceiver getReceiver() {
        return receiver;
    }

    public ChannelSender getSender() {
        return sender;
    }

    public static ThreadFactory getWorkerFactory(){
        return new WorkerFactory();
    }
}
