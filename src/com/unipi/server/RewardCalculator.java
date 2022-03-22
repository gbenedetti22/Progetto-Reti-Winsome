package com.unipi.server;

import com.unipi.common.Console;
import com.unipi.database.EntriesStorage;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.utility.channelsio.ChannelReceiver;
import com.unipi.utility.channelsio.ChannelSender;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RewardCalculator extends Thread {
    private char unit;
    private long timeout;
    private ChannelSender out;
    private ChannelReceiver in;
    private SimpleDateFormat sdf;
    private SocketChannel socket;

    public RewardCalculator() {
        try {
            initClock();
        }catch (NumberFormatException e){
            e.printStackTrace();
            return;
        }

        Map<ServerProperties.NAMES, Object> props = ServerProperties.getValues();
        String dbAddress = (String) props.get(ServerProperties.NAMES.DB_ADDRESS);
        int dbPort = (Integer) props.get(ServerProperties.NAMES.DB_PORT);

        try {
            socket = SocketChannel.open(new InetSocketAddress(dbAddress, dbPort));
            out = new ChannelSender(socket);
            in = new ChannelReceiver(socket);

        } catch (IOException e) {
            e.printStackTrace();
        }

        this.sdf = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
    }

    @Override
    public void run() {
        while (true) {
            try {
                sleep();
                calculateRewards();

            } catch (InterruptedException | IOException e) {
                break;
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculateRewards() throws IOException {
        String command = "PULL NEW ENTRIES";

        out.sendLine(command);
        int size = in.receiveInteger();
        if (size == 0) return;

        for (int i = 0; i < size; i++) {
            EntriesStorage.Entry entry = (EntriesStorage.Entry) in.receiveObject();
            double n1 = 0;
            double n2 = 0;

            HashSet<String> curatori = new HashSet<>();
            String author = entry.HEADER.getAuthor();

            for (Like l : entry.LIKES) {
                if (l.getType() == Like.type.LIKE)
                    n1++;
                else
                    n1 = Math.max(0, --n1);

                curatori.add(l.getUsername());
            }

            n1 = round(Math.log(n1 + 1));

            //i commenti sono partizionati per autore
            long Cp = 0;
            ArrayList<Comment> comments = entry.COMMENTS;
            if (!comments.isEmpty()) {
                for (int j = 0; j <= comments.size() - 1; j++) {
                    if (!comments.get(j).getAuthor().equals(comments.get(Math.min(comments.size() - 1, j + 1)).getAuthor())) {
                        Cp++;
                        n2 += (2 / (1 + Math.pow(Math.E, -(Cp - 1))));
                        Cp = 0;
                        curatori.add(comments.get(j).getAuthor());
                        continue;
                    }

                    Cp++;
                }

                //l ultimo elemento non viene considerato
                n2 += (2 / (1 + Math.pow(Math.E, -(Cp - 1))));
                curatori.add(comments.get(comments.size() - 1).getAuthor());

                n2 = round(Math.log(n2 + 1));
            }

            double reward = (n1 + n2) / entry.HEADER.getInteractions();
            int author_percentage = (int) ServerProperties.getValue(ServerProperties.NAMES.AUTHOR_PERCENTAGE);

            //aggiorni l autore
            double author_reward = round(reward * author_percentage / 100);
            out.sendLine(String.format("UPDATE: %s %s %s", author, author_reward, sdf.format(new Date())));
            String response = in.receiveLine();
            if (!response.equals("OK")) {
                Console.err("Errore nel salvataggio delle ricompense per: " + author);
            }

            //aggiorno i curatori
            double others = round((reward - author_reward) / curatori.size());
            out.sendLine(String.format("UPDATE: %s %s %s", curatori, others, sdf.format(new Date())));
            response = in.receiveLine();
            if (!response.equals("OK")) {
                Console.err(response);
            }
        }

        //inviare in broadcast la notifica
        ServerMain.sendUDPMessage("REWARD-CALCULATED");
    }

    private void sleep() throws InterruptedException {
        switch (unit) {
            case 's' -> TimeUnit.SECONDS.sleep(timeout);
            case 'm' -> TimeUnit.MINUTES.sleep(timeout);
            case 'h' -> TimeUnit.HOURS.sleep(timeout);
            case 'd' -> TimeUnit.DAYS.sleep(timeout);
            case 'w' -> TimeUnit.DAYS.sleep(timeout * 7);
            default -> throw new InterruptedException();
        }

    }

    private void initClock() throws NumberFormatException{
        String delay = (String) ServerProperties.getValues().get(ServerProperties.NAMES.REWARD_TIME_DELAY);
        unit = delay.charAt(delay.length() - 1);

        String time = delay.substring(0, delay.length() - 1);

        timeout = Long.parseLong(time);
        if (timeout <= 0) {
            Console.err("Errore nella conversione del tempo per il calcolo delle rimpense");
            Console.err("Verrà usato il valore di default");

            unit = 's';
            timeout = 5;
        }
    }

    public String getDelay() {
        return String.format("%s%s", timeout, unit);
    }

    private double round(double value) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
