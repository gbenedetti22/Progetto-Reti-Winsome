package winsome.client;

import com.google.gson.Gson;
import winsome.client.mainFrame.MainFrame;
import winsome.server.requestHandler.WSRequest;
import winsome.server.requestHandler.WSResponse;
import winsome.utility.channelsio.ChannelLineReceiver;
import winsome.utility.channelsio.ChannelLineSender;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
/*
    Classe che permette di eseguire richieste asincrone
    Vengono utilizzate 2 blocking queue
    1) requestsQueue -> queue per le richieste
    2) responses queue -> queue per le risposte

    Questa è una classe semplicistica e non adatta per un uso generico.
    Di fatto, quando si vuole fare una richiesta sincrona, il thread che fa la richiesta si mette in ascolto subito
    sulla response queue (produttore-consumatore). E' stata usata questa tecnica per integrare un argomento del corso
    ma in casi reali è bene affidarsi a librerie esterne molto più performanti.
 */
public class ServiceManager extends Thread {
    private LinkedBlockingQueue<Pair> requestsQueue;
    private LinkedBlockingQueue<WSResponse> responsesQueue;
    private boolean running;
    private Gson gson;
    private ChannelLineSender out;
    private ChannelLineReceiver in;
    private SocketChannel socket;

    public ServiceManager(SocketChannel socket) {
        this.requestsQueue = new LinkedBlockingQueue<>();
        this.responsesQueue = new LinkedBlockingQueue<>();
        this.gson = new Gson();
        this.running = true;
        this.out = new ChannelLineSender();
        this.in = new ChannelLineReceiver();
        this.socket = socket;

        out.setChannel(socket);
        in.setChannel(socket);
    }

    public void submitRequest(WSRequest request) {
        requestsQueue.add(new Pair(request));
    }

    // Il parametro callback verrà invocato, con il suo relativo metodo .accept(), quando questo Thread
    // ha ottenuto una risposta
    public void submitRequest(WSRequest request, Consumer<WSResponse> callback) {
        requestsQueue.add(new Pair(request, callback));
    }

    public WSResponse getResponse() {
        try {
            return responsesQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                Pair entry = requestsQueue.take();
                WSRequest request = entry.getRequest();

                String json = gson.toJson(request);
                try {
                    out.sendLine(json);
                } catch (IOException e) {
                    MainFrame.showErrorMessage("Connessione con il Server persa. Riprovare più tardi.");
                    System.exit(0);
                }

                String jsonResponse = in.receiveLine();
                WSResponse response = gson.fromJson(jsonResponse, WSResponse.class);

                if (entry.isCallbackSetted()) {
                    entry.getCallback().accept(response);
                    continue;
                }

                responsesQueue.add(response);
            }

        } catch (InterruptedException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        requestsQueue.clear();
        interrupt();
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reconnect() {
        try {
            SocketAddress address = socket.getRemoteAddress();
            socket.close();

            socket = SocketChannel.open(address);
            out.setChannel(socket);
            in.setChannel(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Pair {
        WSRequest request;
        Consumer<WSResponse> callback;

        public Pair(WSRequest request, Consumer<WSResponse> callback) {
            this.request = request;
            this.callback = callback;
        }

        public Pair(WSRequest request) {
            this(request, null);
        }

        public WSRequest getRequest() {
            return request;
        }

        public Consumer<WSResponse> getCallback() {
            return callback;
        }

        public boolean isCallbackSetted() {
            return callback != null;
        }
    }
}
