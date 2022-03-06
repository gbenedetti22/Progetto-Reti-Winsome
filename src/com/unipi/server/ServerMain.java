package com.unipi.server;

import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.server.RMI.FollowersDatabase;
import com.unipi.server.RMI.FollowersService;
import com.unipi.server.RMI.RegistrationService;
import com.unipi.server.requestHandler.ServerRequestReader;
import com.unipi.server.requestHandler.ServerResponder;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.*;

import static com.unipi.server.ServerProperties.NAMES.*;
import static com.unipi.server.requestHandler.WSRequest.WS_OPERATIONS.GET_FOLLOWERS;

public class ServerMain extends UnicastRemoteObject implements RegistrationService, FollowersService {
    private static final PipedSelector selector = new PipedSelector();
    private static ExecutorService threadPool = Executors.newCachedThreadPool(ServerThreadWorker.getWorkerFactory());
    private static boolean running = true;
    private static ConcurrentHashMap<SocketChannel, String> usersLogged = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, FollowersDatabase> callbacksMap = new ConcurrentHashMap<>();

    protected ServerMain() throws RemoteException {
    }

    public static void main(String[] args) {
        if (!isDatabaseOnline()) {
            System.err.println("Impossibile raggiungere il Database");
            return;
        }

        Map<ServerProperties.NAMES, Object> props = ServerProperties.getValues();

        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.socket().bind(new InetSocketAddress(45678));

            ServerMain registrationService = new ServerMain();

            Registry r1 = LocateRegistry.createRegistry((int) props.get(RMI_REG_PORT));
            r1.bind("REGISTER-SERVICE", registrationService);

            Registry r2 = LocateRegistry.createRegistry((int) props.get(RMI_FOLLOW_PORT));
            r2.bind("FOLLOWERS-SERVICE", registrationService);

            selector.insert(server, SelectionKey.OP_ACCEPT);

            System.out.println("Servizio di registrazione Online");
            System.out.println("Servizio di followers Online");
            System.out.println(running ? "Server Online" : "");
            System.out.println();
            while (running) {
                selector.selectKey(ServerMain::readSelectedKey);
            }

            server.close();
        } catch (IOException | AlreadyBoundException e) {
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
                ServerRequestReader reader = new ServerRequestReader(client, selector);

                Future<?> task = threadPool.submit(reader);
                debugTask(task);
            } else if (key.isWritable()) {
                key.cancel();

                SocketChannel client = (SocketChannel) key.channel();
                WSResponse response = (WSResponse) key.attachment();

                ServerResponder responder = new ServerResponder(client, selector, response);
                Future<?> task = threadPool.submit(responder);
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

    @Override
    public WSResponse performRegistration(WSRequest request) throws RemoteException {
        if (request.getOp() != WSRequest.WS_OPERATIONS.CREATE_USER) {
            return new WSResponse(WSResponse.S_STATUS.ERROR, "Operazione non consentita");
        }

        ServerRequestReader reader = new ServerRequestReader(request);
        /*
         La Java RMI usa dei thread già di suo, quindi perchè usare un thread RMI per far eseguire la registrazione
         al threadpool?

         Perchè, per eseguire la registrazione, devo connettermi al Database e per evitare di aprire
         e chiudere connessioni continuamente, uso quelle già aperte dai Thread nel threadpool.

         Quindi un thread RMI attende la risposta e poi si conclude. Questo perchè non posso agire direttamente
         sui thread usati dall RMI.
         */

        Future<?> task = threadPool.submit(reader);
        try {
            String s = (String) task.get(1500, TimeUnit.MILLISECONDS);
            if (!s.equals("OK")) {
                return new WSResponse(WSResponse.S_STATUS.ERROR, s);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            return new WSResponse(WSResponse.S_STATUS.ERROR, "C'è stato un errore. Riprovare più tardi");
        }

        return WSResponse.newSuccessResponse();
    }

    public static ConcurrentHashMap<SocketChannel, String> getUsersLoggedTable() {
        return usersLogged;
    }

    private static boolean isDatabaseOnline() {
        String dbAddress = (String) ServerProperties.getValues().get(DB_ADDRESS);
        int dbPort = (int) ServerProperties.getValues().get(DB_PORT);

        try (SocketChannel ignored = SocketChannel.open(new InetSocketAddress(dbAddress, dbPort))) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void stop(){
        running = false;
    }

    @Override
    public boolean register(String username, FollowersDatabase callback) throws RemoteException {
        boolean reg = callbacksMap.putIfAbsent(username, callback) == null;
        if(reg) {
            ServerRequestReader request = new ServerRequestReader(new WSRequest(GET_FOLLOWERS, username));
            threadPool.submit(request);
            return true;
        }

        return false;
    }

    @Override
    public boolean unregister(String username) throws RemoteException {
        if (!usersLogged.containsValue(username)) {
            return false;
        }

        callbacksMap.remove(username);
        return true;
    }

    public static FollowersDatabase getCallback(String username) {
        return callbacksMap.get(username);
    }
}
