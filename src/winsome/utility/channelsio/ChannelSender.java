package winsome.utility.channelsio;

import java.io.IOException;

/**
 * Interfaccia che rappresenta uno scrittore di canali.<br>
 * Tutte le classi che la implementano, devono garantire che ad ogni {@link #send(String)}
 * corrisponda una {@link ChannelReceiver#receive()}.<br>
 */
public interface ChannelSender {
    /**
     * Metodo per inviare un messaggio.
     *
     * @param s il messaggio da inviare
     * @throws IOException se avviene qualche errore di I/O
     */
    void send(String s) throws IOException;
}
