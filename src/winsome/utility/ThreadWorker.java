package winsome.utility;

import winsome.utility.channelsio.ChannelLineSender;
import winsome.utility.channelsio.ChannelReceiver;
import winsome.utility.channelsio.ConcurrentChannelLineReceiver;

import java.util.concurrent.ThreadFactory;

/*
    Classe che rappresenta un tipo thread che contiene al suo interno un ChannelSender e un ChannelReceiver, usabili
    dai runnable per comunicare con il client.
 */
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
