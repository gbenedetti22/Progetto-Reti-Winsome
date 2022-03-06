package com.unipi.client;

import com.google.gson.Gson;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;
import com.unipi.utility.channelsio.ChannelReceiver;
import com.unipi.utility.channelsio.ChannelSender;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class ServiceManager extends Thread {
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

    private LinkedBlockingQueue<Pair> requestsQueue;
    private LinkedBlockingQueue<WSResponse> responsesQueue;
    private boolean running;
    private Gson gson;
    private ChannelSender out;
    private ChannelReceiver in;

    public ServiceManager(SocketChannel socket) {
        this.requestsQueue = new LinkedBlockingQueue<>();
        this.responsesQueue = new LinkedBlockingQueue<>();
        this.gson = new Gson();
        this.running = true;
        this.out = new ChannelSender();
        this.in = new ChannelReceiver();

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
                out.sendLine(json);

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
    }
}
