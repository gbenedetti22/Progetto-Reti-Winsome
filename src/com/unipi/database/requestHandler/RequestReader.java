package com.unipi.database.requestHandler;

import com.unipi.database.ThreadWorker;
import com.unipi.channelsio.PipedSelector;
import com.unipi.channelsio.concurrent.ConcurrentChannelReceiver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class RequestReader implements Runnable{
    private SocketChannel socket;
    private PipedSelector selector;

    public RequestReader(SocketChannel socket, PipedSelector selector) {
        this.socket = socket;
        this.selector = selector;
    }

    @Override
    public void run() {
        ThreadWorker worker = (ThreadWorker) Thread.currentThread();
        ConcurrentChannelReceiver in = worker.getReceiver();

        in.setChannel(socket);

        try {
            String message = in.receiveLine();
            System.out.println(message);


            selector.enqueue(socket, SelectionKey.OP_WRITE);
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private void processRequest(String request){

    }

}
