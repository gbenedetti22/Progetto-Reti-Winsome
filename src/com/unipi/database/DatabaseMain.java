package com.unipi.database;

import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.database.requestHandler.Packet;
import com.unipi.database.requestHandler.RequestReader;
import com.unipi.database.requestHandler.RequestWriter;
import com.unipi.database.utility.ThreadWorker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DatabaseMain {
    private static final PipedSelector selector = new PipedSelector();
    private static final Database database = new Database();
    private static ExecutorService threadPool = Executors.newCachedThreadPool(ThreadWorker.getWorkerFactory());
    private static boolean running = true;
    private static int port = 45675;
    private static boolean debug = false;

    public static void main(String[] args) {
        readArgs(args);

        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(port));

            selector.insert(server, SelectionKey.OP_ACCEPT);

            System.out.println(running ? "Database Online" : "");
            System.out.println();
            while (running) {
                selector.selectKey(DatabaseMain::readSelectedKey);
            }

            server.close();
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

    private static void debugTask(Future<?> task) {
        new Thread(() -> {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void readArgs(String[] args) {
        for (String s : args) {
            switch (s.toLowerCase()) {
                case "--clear" -> clearDatabase();
                case "--clear-only" -> {
                    clearDatabase();
                    stop();
                }
                case "--debug" -> debug = true;
                default -> {
                    if (s.startsWith("port")) {
                        String insertedPort = "";
                        try {
                            insertedPort = s.split("=", 2)[1];
                            port = Integer.parseInt(insertedPort);
                        } catch (NumberFormatException e) {
                            System.err.println(insertedPort + ": non è una porta valida");
                            stop();
                        }
                    }
                }
            }
        }

    }

    public static void stop() {
        running = false;

        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void clearDatabase() {
        System.out.println("Avvio pulizia Database...");
        long t1 = System.currentTimeMillis();

        try {
            String dbFolder = "graphDB";
            File[] files = new File(dbFolder).listFiles(f -> !f.isDirectory());
            if (files == null) return;

            for (File file : files) {
                List<String> l = Files.readAllLines(file.toPath());
                String s = l.stream()
                        .filter(p -> !p.startsWith("#"))
                        .map(f -> f.replace("#", ""))
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
        }
    }

    public static Database getDatabase(){
        return database;
    }
}
