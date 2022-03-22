package com.unipi.utility.channelsio;

import java.io.IOException;

/**
 * Interfaccia che rappresenta uno scrittore di canali.<br>
 * Tutte le classi che la implementano, devono garantire che ad ogni {@link #sendLine(String)}
 * corrisponda una {@link Receiver#receiveLine()}.<br>
 */
public interface Sender {
    /**
     * Metodo per inviare un messaggio.
     *
     * @param s il messaggio da inviare
     * @throws IOException se avviene qualche errore di I/O
     */
    void sendLine(String s) throws IOException;

    /**
     * Metodo per bloccare il canale
     */
    void setSleepy();
}
