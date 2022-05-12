package winsome.server;

import winsome.common.Console;
import winsome.database.DBResponse;
import winsome.database.tables.Comment;
import winsome.database.utility.EntriesStorage;
import winsome.utility.channelsio.ChannelLineReceiver;
import winsome.utility.channelsio.ChannelLineSender;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/*
    Classe che si occupa del calcolo delle ricompense
 */
public class RewardCalculator extends Thread {
    private char unit;
    private long timeout;
    private ChannelLineSender out;
    private ChannelLineReceiver in;
    private SimpleDateFormat sdf;
    private SocketChannel socket;

    public RewardCalculator() {
        try {
            initClock();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        Map<ServerProperties.NAMES, Object> props = ServerProperties.getValues();
        String dbAddress = (String) props.get(ServerProperties.NAMES.DB_ADDRESS);
        int dbPort = (Integer) props.get(ServerProperties.NAMES.DB_PORT);

        try {
            socket = SocketChannel.open(new InetSocketAddress(dbAddress, dbPort));
            out = new ChannelLineSender(socket);
            in = new ChannelLineReceiver(socket);

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
        if (size == 0) {
            System.out.println("Non ci sono ricompense da calcolare");
            return;
        }

        // O(new_people_commenting)
        for (int i = 0; i < size; i++) {
            EntriesStorage.Entry entry = (EntriesStorage.Entry) in.receiveObject();
            double n1;
            double n2 = 0;

            String author = entry.HEADER.getAuthor();
            HashSet<String> curatori = entry.HEADER.getCurators();

            // Calcolo dei like
            n1 = Math.max(0, entry.LIKES.size() - entry.DISLIKES.size());
            n1 = round(Math.log(n1 + 1));

            HashMap<String, ArrayList<Comment>> comments = entry.COMMENTS;

            // Calcolo dei commenti
            //Ciclo per new_people_commenting
            for (ArrayList<Comment> newComments : comments.values()) {
                int Cp = newComments.size();
                n2 += (2 / (1 + Math.pow(Math.E, -(Cp - 1))));
            }

            n2 = round(Math.log(n2 + 1));
            double reward = (n1 + n2) / entry.HEADER.getInteractions();
            Console.log("Calcolata ricompensa", reward);
            int author_percentage = (int) ServerProperties.getValue(ServerProperties.NAMES.AUTHOR_PERCENTAGE);

            // Assegno la ricompense al creatore
            double author_reward = round(reward * author_percentage / 100);
            out.sendLine(String.format("UPDATE: %s %s %s %s", author, author_reward, sdf.format(new Date()), entry.HEADER.getIdPost().toString()));
            DBResponse response = (DBResponse) in.receiveObject();
            if (!response.getCode().equals(DBResponse.OK)) {
                Console.err("Errore nel salvataggio delle ricompense per: " + author);
            }

            //aggiorno le ricompense per i curatori
            double others = round((reward - author_reward) / curatori.size());
            out.sendLine(String.format("UPDATE: %s %s %s", curatori, others, sdf.format(new Date())));
            response = (DBResponse) in.receiveObject();
            if (!response.getCode().equals(DBResponse.OK)) {
                Console.err(response);
            }
        }

        //inviare in broadcast la notifica
        ServerMain.sendUDPMessage("REWARD-CALCULATED");
    }

    // unit può essere tipo: 1w, 5s, 2m ecc ergo "l unità di misura" è sempre l ultimo carattere
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

    // metodo che controlla di aver inserito correttamente un valore valido per il tempo di timeout
    private void initClock() throws NumberFormatException {
        String delay = (String) ServerProperties.getValues().get(ServerProperties.NAMES.REWARD_TIME_DELAY);
        unit = delay.charAt(delay.length() - 1);

        String time = delay.substring(0, delay.length() - 1);

        try {
            timeout = Long.parseLong(time);
        } catch (NumberFormatException e) {
            System.err.println("Inserito un valore non valido per il tempo di attesa");
            System.err.println("Verrà usato il valore di default");
            timeout = 1;
            unit = 'w';
            return;
        }

        if (timeout <= 0) {
            System.err.println("Errore nella conversione del tempo per il calcolo delle rimpense");
            System.err.println("Verrà usato il valore di default");

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
