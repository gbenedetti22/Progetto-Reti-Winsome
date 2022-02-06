package com.unipi.database;

import com.unipi.channelsio.PipedSelector;
import com.unipi.database.requestHandler.RequestReader;
import com.unipi.database.requestHandler.RequestWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DatabaseMain {
    private static boolean running = true;
    private static final PipedSelector selector = new PipedSelector();
    private static ExecutorService threadPool = Executors.newCachedThreadPool(ThreadWorker.getWorkerFactory());

    public static void main(String[] args) throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(45678));

        selector.insert(server, SelectionKey.OP_ACCEPT);

        System.out.println("Server Online");
        System.out.println();
        while (running) {
            selector.selectKey(DatabaseMain::readSelectedKey);
        }

        selector.close();
        server.close();
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
                RequestReader reader = new RequestReader(client, selector);

                Future<?> task = threadPool.submit(reader);
                debugTask(task);

            }else if(key.isWritable()){
                key.cancel();

                SocketChannel client = (SocketChannel) key.channel();
                RequestWriter writer = new RequestWriter(client, selector);

                Future<?> task = threadPool.submit(writer);
                debugTask(task);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void debugTask(Future<?> task) {
        new Thread(()->{
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void stop() {
        running = false;
    }
}
