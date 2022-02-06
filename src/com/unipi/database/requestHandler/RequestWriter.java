package com.unipi.database.requestHandler;

import com.unipi.database.ThreadWorker;
import com.unipi.channelsio.ChannelSender;
import com.unipi.channelsio.PipedSelector;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RequestWriter implements Runnable{
    private SocketChannel socket;
    private PipedSelector selector;

    public RequestWriter(SocketChannel socket, PipedSelector selector) {
        this.socket = socket;
        this.selector = selector;
    }

    @Override
    public void run() {
        ThreadWorker worker = (ThreadWorker) Thread.currentThread();
        ChannelSender out = worker.getSender();

        out.setChannel(socket);

        try {
            out.sendLine("Ricevuto");

            selector.enqueue(socket, SelectionKey.OP_READ);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
