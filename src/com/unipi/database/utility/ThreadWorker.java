package com.unipi.database.utility;

import com.unipi.utility.channelsio.ChannelSender;
import com.unipi.utility.channelsio.ConcurrentChannelReceiver;
import com.unipi.utility.channelsio.Receiver;

import java.util.concurrent.ThreadFactory;

public class ThreadWorker extends Thread {
    private ConcurrentChannelReceiver receiver;
    private ChannelSender sender;
    public ThreadWorker(Runnable target) {
        super(target);

        receiver = Receiver.newConcurrentReceiver();
        sender = new ChannelSender();
    }

    public static ThreadFactory getWorkerFactory() {
        return new WorkerFactory();
    }

    public ConcurrentChannelReceiver getReceiver() {
        return receiver;
    }

    public ChannelSender getSender() {
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
