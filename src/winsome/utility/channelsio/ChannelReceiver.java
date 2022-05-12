package winsome.utility.channelsio;

import java.io.IOException;

/**
 * Interfaccia che rappresenta un ricevitore dal canale.<br>
 * Tutte le classi che la implementano, devono garantire il metodo per leggere una riga di testo dal canale.<br>
 */
public interface ChannelReceiver {
    /**
     * Metodo statico per la creazione di un nuovo Receiver concorrente (vedi {@link ConcurrentChannelLineReceiver})
     *
     * @return un nuovo {@link ConcurrentChannelLineReceiver}
     */
    static ConcurrentChannelLineReceiver newConcurrentReceiver() {
        return ConcurrentReceiverBuilder.newConcurrentReceiver();
    }

    /**
     * Metodo statico per la creazione di un nuovo Receiver concorrente (vedi {@link ChannelLineReceiver})
     *
     * @return un nuovo {@link ChannelLineReceiver}
     */
    static ChannelLineReceiver newReceiver() {
        return new ChannelLineReceiver();
    }

    /**
     * Metodo per ricevere un messaggio inviato da {@link ChannelSender#send(String)}.
     *
     * @return la stringa inviata con il metodo {@link ChannelSender#send(String)}
     * @throws IOException se avviene qualche errore di I/O
     */
    String receive() throws IOException;
}
