package com.unipi.server.requestHandler;

import com.google.gson.Gson;
import com.unipi.utility.channelsio.ChannelSender;
import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.server.ServerThreadWorker;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ServerResponder implements Runnable{
    private SocketChannel socket;
    private PipedSelector selector;
    private WSResponse response;
    private Gson gson;

    public ServerResponder(SocketChannel socket, PipedSelector selector, WSResponse response) {
        this.socket = socket;
        this.selector = selector;
        this.response = response;
        this.gson = new Gson();
    }


    @Override
    public void run() {
        ServerThreadWorker worker = (ServerThreadWorker) Thread.currentThread();
        ChannelSender out = worker.getSender();
        out.setChannel(socket);

        String responseJson = gson.toJson(response);
        try {
            out.sendLine(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            selector.enqueue(socket, SelectionKey.OP_READ);
        }

    }
}
