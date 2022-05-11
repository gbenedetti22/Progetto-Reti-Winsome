package com.unipi.server;

import com.unipi.utility.ThreadWorker;
import com.unipi.utility.channelsio.ChannelLineSender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import static com.unipi.server.ServerProperties.NAMES.CLOSE_DB;

/*
    Classe che estende le funzionalità del thread worker.
    Oltre ad avere un ChannelSender e un ChannelReceiver (che eredita dalla classe padre),
    contiene anche il Socket connesso al Database.
 */
public class ServerThreadWorker extends ThreadWorker {

    private SocketChannel socket;

    public ServerThreadWorker(Runnable target, SocketChannel socket) {
        super(target);
        this.socket = socket;
    }

    public static ThreadFactory getWorkerFactory() {
        return new ServerWorkerFactory();
    }

    public SocketChannel getDatabaseSocket() {
        return socket;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        System.out.println("Interrotto Thread: " + getName());
        String value = (String) ServerProperties.getValue(CLOSE_DB);
        boolean close_db = Boolean.parseBoolean(value);
        if (!close_db) return;

        try {
            ChannelLineSender out = new ChannelLineSender(socket);
            try {
                System.out.println("Chiusura Database..");
                out.sendLine("CLOSE");
            } catch (IOException ignored) {
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class ServerWorkerFactory implements ThreadFactory {
        private long id = 0;

        @Override
        public Thread newThread(Runnable r) {
            SocketChannel socket;
            try {
                socket = connect();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            ServerThreadWorker worker = new ServerThreadWorker(r, socket);

            worker.setName("WORKER - " + id++);
            worker.setPriority(Thread.NORM_PRIORITY);
            worker.setDaemon(false);
            return worker;
        }

        private SocketChannel connect() throws IOException {
            Map<ServerProperties.NAMES, Object> props = ServerProperties.getValues();
            String dbAddress = (String) props.get(ServerProperties.NAMES.DB_ADDRESS);
            int dbPort = (Integer) props.get(ServerProperties.NAMES.DB_PORT);

            return SocketChannel.open(new InetSocketAddress(dbAddress, dbPort));
        }
    }
}
