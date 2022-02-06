package com.unipi.channelsio;

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
}
