package com.unipi.database.utility;

import com.unipi.common.Console;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.utility.StandardPriority;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PriorityAsyncExecutor extends Thread {
    private static class Entry implements Comparable<Entry> {
        Runnable runnable;
        StandardPriority priority;

        public Entry(Runnable runnable, StandardPriority priority) {
            this.runnable = runnable;
            this.priority = priority;
        }

        @Override
        public int compareTo(Entry o) {
            if (o == this) return 0;

            int compare = priority.compareTo(o.priority);
            return compare == 0 ? 1 : compare;
        }
    }

    private Database database;
    private char unit;
    private long timeout;
    private String delay;
    private PriorityBlockingQueue<Entry> queue;
    private ReentrantLock lock;
    private Condition saver;
    private Condition worker;

    public PriorityAsyncExecutor(String delay) {
        this.queue = new PriorityBlockingQueue<>();
        this.database = DatabaseMain.getDatabase();
        this.delay = delay;
        this.lock = new ReentrantLock();
        this.saver = lock.newCondition();
        this.worker = lock.newCondition();

        try {
            initClock();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!interrupted()) {
            try {
                sleep();
                save();
            } catch (InterruptedException e) {
                break;
            }
        }

        save();
    }

    private void save() {
        System.out.println("Inizio salvataggio...");
        database.save();

        while (!queue.isEmpty()) {
            Entry e = queue.poll();
            e.runnable.run();

            lock.lock();
            if (lock.hasWaiters(worker)) {
                worker.signal();
            }
            lock.unlock();
        }

        System.out.println("Salvataggio completato!");
    }

    public void asyncExecute(Runnable runnable, StandardPriority priority) {
        while (queue.size() == Integer.MAX_VALUE) {
            try {
                lock.lock();
                saver.signal();
                worker.await();

            } catch (InterruptedException e) {
                interrupt();
                return;
            } finally {
                lock.unlock();
            }
        }

        queue.add(new Entry(runnable, priority));
    }


    private void sleep() throws InterruptedException {
        lock.lock();
        try {
            switch (unit) {
                case 's' -> saver.await(timeout, TimeUnit.SECONDS);
                case 'm' -> saver.await(timeout, TimeUnit.MINUTES);
                case 'h' -> saver.await(timeout, TimeUnit.HOURS);
                case 'd' -> saver.await(timeout, TimeUnit.DAYS);
                case 'w' -> saver.await(timeout * 7, TimeUnit.DAYS);
                default -> throw new InterruptedException();
            }
        } finally {
            lock.unlock();
        }

    }

    private void initClock() throws NumberFormatException {
        unit = delay.charAt(delay.length() - 1);

        String time = delay.substring(0, delay.length() - 1);
        timeout = Long.parseLong(time);
        if (timeout <= 0) {
            Console.err("Errore nella conversione del tempo per il calcolo delle rimpense");
            Console.err("Verrà usato il valore di default");

            unit = 's';
            timeout = 5;
        }
    }
}
