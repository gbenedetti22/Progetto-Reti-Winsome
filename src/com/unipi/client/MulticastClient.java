package com.unipi.client;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.unipi.client.mainFrame.MainFrame;
import com.unipi.common.Console;
import com.unipi.common.WinsomeTransaction;
import com.unipi.server.ServerMain;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.*;
import java.util.LinkedList;

public class MulticastClient extends Thread {
    private String address;
    private int port;
    private MulticastSocket multicastSocket;
    private boolean opened;
    private NetworkInterface networkInterface;
    private ServiceManager serviceManager;
    private Gson gson;
    private LocalStorage storage;

    public MulticastClient(String address, int port, ServiceManager serviceManager, LocalStorage storage) {
        this.address = address;
        this.port = port;
        this.serviceManager = serviceManager;
        this.gson = new Gson();
        this.storage = storage;
        try {
            this.networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()));
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

        opened = true;
    }

    @Override
    public void run() {
        try {
            multicastSocket = new MulticastSocket(port);
            try {
//            multicastSocket.joinGroup(InetAddress.getByName(address));
                multicastSocket.joinGroup(new InetSocketAddress(address, port), networkInterface);
            }catch (IOException e) {
                MainFrame.showErrorMessage("Impossibile ricevere notifiche sullo status delle ricompense");
                return;
            }

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

            while (!isInterrupted() && opened) {
                try {
                    multicastSocket.receive(packet);
                } catch (SocketException e) {
                    break;
                }

                if (multicastSocket.isClosed() || !opened) break;

                String s = new String(packet.getData(), 0, packet.getLength());
                Console.log(s);

                if (s.equals("REWARD-CALCULATED")) {
                    //richiesta al server per ricevere quanti coins ho
                    serviceManager.submitRequest(new WSRequest(WSRequest.WS_OPERATIONS.GET_TRANSACTIONS));
                    WSResponse response = serviceManager.getResponse();
                    if (response.code() != WSResponse.CODES.OK) {
                        MainFrame.showErrorMessage(response.getBody());
                        return;
                    }

                    Type type = new TypeToken<LinkedList<WinsomeTransaction>>() {
                    }.getType();
                    LinkedList<WinsomeTransaction> transactions = gson.fromJson(response.getBody(), type);
                    Pages.PROFILE_PAGE.setTransactions(transactions);
                    Pages.PROFILE_PAGE.setWinsomeCoins(transactions.getLast().getCoins());

                    JOptionPane.showMessageDialog(null, "Hai ricevuto dei nuovi Winsome Coins!\n" +
                            "Vai nel tuo profilo e controlla le tue transizioni :)");

                } else if(s.startsWith("REMOVED REWIN")) {

                    //REMOVED REWIN AUTHOR ID
                    String[] split = s.split(" ", 4);
                    String username = split[2];
                    String id = split[3];
                    Console.log(username);
                    Console.log(id);
                    if(username.equals(storage.getCurrentUsername())) continue;

                    Pages.HOME_PAGE.removePostIf(p -> p.getRewin()!= null && p.getRewin().equals(username) && p.getID().equals(id));
                } else if (s.startsWith("REMOVED")) {

                    //REMOVED USERNAME ID
                    String[] split = s.split(" ", 3);
                    String username = split[1];
                    String id = split[2];
                    if(username.equals(storage.getCurrentUsername())) continue;

                    Pages.HOME_PAGE.removePostIf(p -> p.getID().equals(id));
                    Pages.PROFILE_PAGE.removePostIf(p -> p.getID().equals(id));
                }

            }

            multicastSocket.leaveGroup(new InetSocketAddress(address, port), networkInterface);
            if (!multicastSocket.isClosed())
                multicastSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close() {
        opened = false;
        multicastSocket.close();
        interrupt();
    }
}
