package com.unipi.database.utility;

import com.unipi.common.Console;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.utility.StandardPriority;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/*
    Classe che permette il salvataggio asincrono sul disco.
    Viene fatto uso di questa classe per fare in modo che sia un thread solo ad accedere sul disco
    e che i thread worker possano occuparsi delle richieste.

    Viene fatto uso di una PriorityBlockingQueue per gestire i salvataggi asincroni.
    Il tempo di attesa tra un salvataggio e l'altro è definibile nel file di configurazione del database.
    Se la queue è troppo piena, il thread worker segnalerà la cosa al PriorityAsyncSaver e lui comincierà subito a svuotarla (svegliandosi prematuramente).
 */

public class PriorityAsyncSaver extends Thread {
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

    public PriorityAsyncSaver(String delay) {
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

            lock.lock();
            if (lock.hasWaiters(worker)) {
                worker.signal();    //Se ci sono dei thread in attesa, notifico che adesso c'è un posto libero
            }
            lock.unlock();

            e.runnable.run();
        }

        System.out.println("Salvataggio completato!");
        System.out.println("\n===========================================================\n");
    }

    public void asyncSave(Runnable runnable, StandardPriority priority) {
        // se la queue è piena, mando un segnale, sveglio il PriorityAsyncSaver e attendo che ci sia posto nella queue
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
