package com.unipi.database.utility;

import com.unipi.utility.channelsio.ChannelLineSender;
import com.unipi.utility.channelsio.ConcurrentChannelLineReceiver;
import com.unipi.utility.channelsio.ChannelReceiver;

import java.util.concurrent.ThreadFactory;

public class ThreadWorker extends Thread {
    private ConcurrentChannelLineReceiver receiver;
    private ChannelLineSender sender;
    public ThreadWorker(Runnable target) {
        super(target);

        receiver = ChannelReceiver.newConcurrentReceiver();
        sender = new ChannelLineSender();
    }

    public static ThreadFactory getWorkerFactory() {
        return new WorkerFactory();
    }

    public ConcurrentChannelLineReceiver getReceiver() {
        return receiver;
    }

    public ChannelLineSender getSender() {
        return sender;
    }

    private static class WorkerFactory implements ThreadFactory {
        private long id = 0;

        @Override
        public Thread newThread(Runnable r) {
            ThreadWorker worker = new ThreadWorker(r);

            worker.setName("WORKER - " + id++);
            worker.setPriority(Thread.NORM_PRIORITY);
            worker.setDaemon(false);
            return worker;
        }
    }
}
