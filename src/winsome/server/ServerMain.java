package winsome.server;

import winsome.server.RMI.FollowersDatabase;
import winsome.server.RMI.FollowersService;
import winsome.server.RMI.RegistrationService;
import winsome.server.requestHandler.ServerRequestReader;
import winsome.server.requestHandler.ServerResponder;
import winsome.server.requestHandler.WSRequest;
import winsome.server.requestHandler.WSResponse;
import winsome.utility.channelsio.PipedSelector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.*;

public class ServerMain extends UnicastRemoteObject implements RegistrationService, FollowersService {
    private static final PipedSelector selector = new PipedSelector();
    private static ExecutorService threadPool = Executors.newCachedThreadPool(ServerThreadWorker.getWorkerFactory());
    private static ConcurrentHashMap<SocketChannel, String> usersLogged = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, FollowersDatabase> callbacksMap = new ConcurrentHashMap<>();

    private static DatagramSocket udpSocket;

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
            udpSocket = new DatagramSocket();

            Registry r1 = LocateRegistry.createRegistry((int) props.get(ServerProperties.NAMES.RMI_REG_PORT));
            r1.bind("REGISTER-SERVICE", registrationService);

            Registry r2 = LocateRegistry.createRegistry((int) props.get(ServerProperties.NAMES.RMI_FOLLOW_PORT));
            r2.bind("FOLLOWERS-SERVICE", registrationService);

            RewardCalculator rewardCalculator = new RewardCalculator();
            rewardCalculator.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Avvio chiusura...");
                System.out.println("Chiusura threadpool...");
                threadPool.shutdownNow();
                udpSocket.close();

                try {
                    System.out.println("Chiusura selector...");
                    selector.close();

                    System.out.println("Chiusura servizi RMI...");
                    r1.unbind("REGISTER-SERVICE");
                    r2.unbind("FOLLOWERS-SERVICE");

                    System.out.println("Chiusura Reward Calculator...");
                    rewardCalculator.interrupt();
                } catch (IOException | NotBoundException e) {
                    e.printStackTrace();
                }

                System.out.println("Bye bye :)");
            }));

            selector.insert(server, SelectionKey.OP_ACCEPT);

            System.out.println("Servizio di registrazione Online");
            System.out.println("Servizio di calcolo delle ricompense Online");
            System.out.println("Delay: " + rewardCalculator.getDelay());
            System.out.println("Servizio di followers Online");
            System.out.println("Server Online");
            System.out.println();
            while (true) {
                selector.selectKey(ServerMain::readSelectedKey);
            }

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

    // funzione di puro debug. Se il task fallisce viene lanciata un eccezione
    public static void debugTask(Future<?> task) {
        new Thread(() -> {
            try {
                task.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static ConcurrentHashMap<SocketChannel, String> getUsersLoggedTable() {
        return usersLogged;
    }

    // funzione per stabilire se il database ?? online.
    // viene aperta e chiusa una connessione.
    private static boolean isDatabaseOnline() {
        String dbAddress = (String) ServerProperties.getValues().get(ServerProperties.NAMES.DB_ADDRESS);
        int dbPort = (int) ServerProperties.getValues().get(ServerProperties.NAMES.DB_PORT);

        try (SocketChannel ignored = SocketChannel.open(new InetSocketAddress(dbAddress, dbPort))) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    // Questo NON ?? il metodo usato per la de-registrazione di un client per le callback
    // Questo viene chiamato se un client chiude la connessione senza de-registrarsi
    public static void unregisterClient(String username) {
        if (username == null) return;

        callbacksMap.remove(username);
    }

    public static FollowersDatabase getCallback(String username) {
        return callbacksMap.get(username);
    }

    public static void sendUDPMessage(String message) {
        try {

            InetAddress address = InetAddress.getByName((String) ServerProperties.getValue(ServerProperties.NAMES.MULTICAST_ADDRESS));
            int port = (int) ServerProperties.getValue(ServerProperties.NAMES.MULTICAST_PORT);
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), address, port);
            udpSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo chiamato in remoto per effettuare la registrazione
    // L unica operazione consentita ?? ovviamente la "create user".
    // A differenza della registrazione per il servizio di notifica sui followers, qua il thread attende una risposta.
    // Questo perch??, prima di poter effettuare il login con il nuovo account, devo poter sapere se ?? "andato tutto bene"
    @Override
    public WSResponse performRegistration(WSRequest request) throws RemoteException {
        if (request.getOp() != WSRequest.WS_OPERATIONS.CREATE_USER) {
            return new WSResponse(WSResponse.CODES.ERROR, "Operazione non consentita");
        }

        ServerRequestReader reader = new ServerRequestReader(request);
        /*
         Per eseguire la registrazione, devo connettermi al Database e per evitare di aprire
         e chiudere connessioni continuamente, uso quelle gi?? aperte dai Thread nel threadpool.

         Quindi un thread RMI attende la risposta e poi si conclude. Questo perch?? non posso agire direttamente
         sui thread usati dall RMI.
         */

        Future<?> task = threadPool.submit(reader);
        try {
            String s = (String) task.get(1500, TimeUnit.MILLISECONDS);
            if (!s.equals("OK")) {
                return new WSResponse(WSResponse.CODES.ERROR, s);
            }

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            return new WSResponse(WSResponse.CODES.ERROR, "C'?? stato un errore. Riprovare pi?? tardi");
        }

        return WSResponse.newSuccessResponse();
    }

    // Metodo per registrarsi al servizio di follow
    // Nell esatto momento in cui un client si registra, viene fatta una richiesta asincrona per ricevere i followers.

    // Da notare che viene usato il thread pool del Server perch??:
    // 1) tutti i thread di esso hanno gi?? una connessione stabilita con il Database
    // 2) mi permette di uscire velocemente
    // 3) non creo nuovi connessioni per ogni client, cosa inutile per via del punto 1
    @Override
    public boolean register(String username, FollowersDatabase callback) throws RemoteException {
        boolean reg = callbacksMap.putIfAbsent(username, callback) == null;
        if (reg) {
            ServerRequestReader request = new ServerRequestReader(new WSRequest(WSRequest.WS_OPERATIONS.GET_FOLLOWERS, username));
            threadPool.submit(request);
            return true;
        }

        return false;
    }

    // Metodo per de-registrarsi al servizio di follow
    @Override
    public boolean unregister(String username) throws RemoteException {
        if (username == null) return false;

        if (!usersLogged.containsValue(username)) {
            return false;
        }

        callbacksMap.remove(username);
        return true;
    }
}
