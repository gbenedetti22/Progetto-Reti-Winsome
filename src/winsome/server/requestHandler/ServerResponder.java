package winsome.server.requestHandler;

import com.google.gson.Gson;
import winsome.server.ServerThreadWorker;
import winsome.utility.channelsio.ChannelLineSender;
import winsome.utility.channelsio.PipedSelector;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/*
    Classe che gestisce le risposte del server
    Semplicemente riceve i messaggi dalla classe ServerRequestReader e li invia al client in formato JSON
 */
public class ServerResponder implements Runnable {
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
        ChannelLineSender out = worker.getSender();
        out.setChannel(socket);

        String responseJson = gson.toJson(response);
        try {
            out.sendLine(responseJson);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            selector.enqueue(socket, SelectionKey.OP_READ);
        }

    }
}
