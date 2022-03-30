package com.unipi.client;

import com.google.gson.Gson;
import com.unipi.client.mainFrame.MainFrame;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;
import com.unipi.utility.channelsio.ChannelLineReceiver;
import com.unipi.utility.channelsio.ChannelLineSender;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

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
                }catch (IOException e){
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
