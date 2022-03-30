package com.unipi.database;

import com.unipi.database.requestHandler.Packet;
import com.unipi.database.requestHandler.RequestReader;
import com.unipi.database.requestHandler.RequestWriter;
import com.unipi.database.utility.ThreadWorker;
import com.unipi.utility.channelsio.PipedSelector;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.unipi.server.ServerMain.debugTask;

public class DatabaseMain {
    private static final PipedSelector selector = new PipedSelector();
    private static Database database;
    private static ExecutorService threadPool = Executors.newCachedThreadPool(ThreadWorker.getWorkerFactory());
    private static int port = 45675;
    private static boolean debug = false;
    private static boolean running = true;
    private static String delay = "5m";
    private static Thread closingThread;

    public static void main(String[] args) {
        readProp();
        database = new Database();
        System.out.println("Delay salvataggio: " + delay);
        database.startSaving(delay);

        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(port));

            selector.insert(server, SelectionKey.OP_ACCEPT);

            closingThread = new Thread(() -> {
                try {
                    System.out.println("Chiusura Database..");
                    database.close();

                    System.out.println("Chiusura connessioni...");
                    server.close();
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.println("Bye bye :)");
            });
            closingThread.setName("CLOSING-THREAD");
            Runtime.getRuntime().addShutdownHook(closingThread);

            System.out.println("Database Online");
            System.out.println();
            while (running) {
                selector.selectKey(DatabaseMain::readSelectedKey);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readSelectedKey(SelectionKey key) {
        try {
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel client = server.accept();

                selector.insert(client, SelectionKey.OP_READ);
            } else if (key.isReadable()) {
                key.cancel();

                SocketChannel client = (SocketChannel) key.channel();
                RequestReader reader = new RequestReader(client, selector, debug);

                Future<?> task = threadPool.submit(reader);
                debugTask(task);
            } else if (key.isWritable()) {
                key.cancel();

                SocketChannel client = (SocketChannel) key.channel();
                RequestWriter writer = new RequestWriter(client, selector, (Packet) key.attachment());

                Future<?> task = threadPool.submit(writer);
                debugTask(task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readProp() {
        Properties prop = new Properties();

        try {
            FileReader reader = new FileReader("prop.properties");
            prop.load(reader);
            reader.close();

            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                switch (entry.getKey().toString().toLowerCase()) {
                    case "clear" -> {
                        if(entry.getValue().toString().equals("true")) {
                            clearDatabase();
                        }
                    }
                    case "debug" -> debug = true;
                    case "port" -> {
                        String insertedPort = "";
                        try {
                            insertedPort = entry.getValue().toString();
                            port = Integer.parseInt(insertedPort);
                        } catch (NumberFormatException e) {
                            System.err.println(insertedPort + ": non è una porta valida");
                            System.err.println("Verrà usato il valore di default");
                        }
                    }

                    case "delay" -> delay = entry.getValue().toString();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //funzione molto rudimentale, serve più a dare l idea che ad essere efficiente
    private static void clearDatabase() {
        System.out.println("===========================================================\n");
        System.out.println("Avvio pulizia Database...");
        long t1 = System.currentTimeMillis();

        try {
            String dbFolder = Database.getName();
            File[] files = new File(dbFolder).listFiles(f -> !f.isDirectory());
            if (files == null) return;

            for (File file : files) {
                if (file.getName().equals("rewins")) continue;
                if (file.getName().equals("users.json")) continue;
                if (file.getName().equals("posts.json")) continue;

                List<String> l = Files.readAllLines(file.toPath());
                String s = l.stream()
                        .filter(p -> !p.startsWith("#"))
                        .map(f -> f.replace("#", ""))
                        .map(f -> f.endsWith(";") ? f.substring(0, f.lastIndexOf(';')) : f)
                        .collect(Collectors.joining("\n"));

                if (!s.endsWith("\n") && !s.isBlank()) s = s.concat("\n");

                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(s);
                out.close();
            }
        } catch (IOException ignored) {

        } finally {
            long t2 = System.currentTimeMillis();
            System.out.println("Pulizia completata!");
            System.out.println("Tempo: " + (t2 - t1) + "ms");
            System.out.println("\n===========================================================\n");
        }
    }

    public static Database getDatabase() {
        return database;
    }

    public synchronized static void safeClose() {
        if (!running) return;

        running = false;
        Runtime.getRuntime().removeShutdownHook(closingThread);
        closingThread.start();
        try {
            closingThread.join();
            System.exit(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
