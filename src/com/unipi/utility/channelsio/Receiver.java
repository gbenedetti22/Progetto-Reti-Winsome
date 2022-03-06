package com.unipi.utility.channelsio;

import com.unipi.utility.channelsio.concurrent.ConcurrentChannelReceiver;
import com.unipi.utility.channelsio.concurrent.ConcurrentReceiverBuilder;

import java.io.IOException;

/**
 * Interfaccia che rappresenta un ricevitore dal canale.<br>
 * Tutte le classi che la implementano, devono garantire il metodo per leggere una riga di testo dal canale.<br>
 */
public interface Receiver {
    /**
     * Metodo per ricevere un messaggio inviato da {@link Sender#sendLine(String)}.
     * @throws IOException se avviene qualche errore di I/O
     */
    String receiveLine() throws IOException;
    void setSleepy();

    /**
     * Metodo statico per la creazione di un nuovo Receiver concorrente (vedi {@link ConcurrentChannelReceiver})
     *
     * @return un nuovo {@link ConcurrentChannelReceiver}
     */
    static ConcurrentChannelReceiver newConcurrentReceiver() {
        return ConcurrentReceiverBuilder.newConcurrentReceiver();
    }
}
