package com.unipi.utility.channelsio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Base64;
import java.util.HashMap;

/**
 * Classe che permette di ricevere i dati inviati da {@link ChannelSender} (su uno o più canali).<br>
 * Il metodo {@link #receiveLine()} garantisce che ad ogni chiamata di {@link ChannelSender#sendLine(String)} sia associata
 * una {@link #receiveLine()}. <br>Questo permette di oviare al tipico problema di lettura dei canali, dove si va a leggere
 * quello che è immediatamente presente, basandosi sulla dimensione del buffer.<br>
 * Es. {@link ChannelReceiver} vs {@link SocketChannel}<br>
 * ChannelSender.sendLine("ciao");<br>
 * ChannelSender.sendLine("mondo");<br>
 * <br>
 * ChannelReceiver.readLine(); -> restituirà "ciao"<br>
 * ChannelReceiver.readLine(); -> restituirà "mondo"<br>
 * <br>
 * ==========================================================<br>
 * SocketChannel.write("ciao");<br>
 * SocketChannel.write("mondo");<br>
 * <br>
 * SocketChannel.read(); -> può restituire "ciao", "ciao mon", "ciao mondo" ecc<br>
 * SocketChannel.read(); -> può generare errori<br>
 * <br>
 * Il chiamante è invitato a non allocare di continuo nuovi {@link ChannelSender}/{@link ChannelReceiver} in quanto, ad oogni nuova allocazione,
 * vengono allocati nuovi buffer e ciò potrebbe comportare in un calo di prestazioni.<br>{@link ChannelSender} e {@link ChannelReceiver}
 * sono stati costruiti per poter funzionare anche su più canali diversi chiamando il metodo {@link #setChannel(SocketChannel)}<br>
 * <br>
 * Questa classe <u>NON</u> è thread-safe e deve essere usata da <u>UN</u> thread solo.<br>
 * Per l uso su più thread vedere la versione {@link ConcurrentChannelReceiver}.<br>
 */
public class ChannelReceiver implements Receiver {
    // Mappa che permette il salvataggio degli eccessivi dati letti
    // Supponiamo che il mittente voglia inviare "ciao" e poi "mondo". Se dal canale
    // leggo "ciao mon", "ciao" viene restituito e "mon" viene salvato dentro la mappa.
    // Alla successiva iterazione, "mon" viene ripreso.
    private static HashMap<SocketChannel, String> chunks = new HashMap<>();
    private ByteBuffer buffer;
    private SocketChannel channel;
    private StringBuilder builder;
    private final String SEPARATOR = System.lineSeparator();

    /**
     * Costruisce un Receiver che si metterà in ascolto su channel.<br>
     *
     * @param channel il canale di ascolto
     */
    public ChannelReceiver(SocketChannel channel) {
        this.channel = channel;

        buffer = ByteBuffer.allocate(10);
        builder = new StringBuilder();
    }

    /**
     * Costruisce un Receiver che non ascolta su nessun canale.<br>
     * Successive invocazioni al metodo {@link #getListeningChannel()} restituiranno null.
     */
    public ChannelReceiver() {
        this(null);
    }

    /**
     * Riceve una riga di testo inviata da {@link ChannelSender#sendLine(String)}.<br>
     * Il canale d ascolto è quello settato mediante setChannel() o quello passato al costruttore<br>
     * <br>
     *
     * @return la riga di testo inviata mediante {@link ChannelSender#sendLine(String)} o null se il canale è chiuso,
     * se EOS è stato raggiunto o se è in modalità dormiente. vedi {@link #isSleepy()}<br>
     * @throws IOException se avviene qualche errore di I/O<br>
     */
    public String receiveLine() throws IOException {
        if (channel == null || !channel.isOpen()) return null;

        builder.setLength(0);   //pulisco il buffer

        String chunk = chunks.get(channel);
        if (chunk != null) {  //se nell iterazione precedente ho letto più del dovuto, riprendo da dove ero rimasto
            builder.append(chunk);
            chunks.remove(channel);
        }

        int nBytes = 0;
        int offset = 0;

        //Continuo a leggere finchè non trovo, nel testo ricevuto, lineSeparetor
        while (builder.indexOf(SEPARATOR, (offset - nBytes) - SEPARATOR.length()) == -1) {
            nBytes = channel.read(buffer);

            if (nBytes < 0) {
                buffer.clear();
                return null;
            }

            // la guardia del while corrisponde ESATTAMENTE a:
            // new String(buffer.array(), 0, nBytes).contains(lineSeparetor);

            // non ho usato la String.contains per evitare di dover mettere un while(true)
            builder.append(new String(buffer.array(), 0, nBytes));

            offset += nBytes;

            // il buffer viene sempre pulito per rendere indipendente la capacità del buffer
            // dalla lunghezza del testo ricevuto. Cioè non importa quanto sia grande il buffer, io devo sempre
            // essere nelle condizioni di poter ricevere
            buffer.clear();
        }

        // da qui in poi, il testo inviato dalla ChannelSender.sendLine() è ricevuto.
        // Se ho ricevuto di più, questa "spazzatura" viene salvata per le successive iterazioni
        String message = builder.toString();
        String[] data = message.split(System.lineSeparator(), 2);
        message = data[0];

        if (data.length > 1 && !data[1].isBlank()) {
            String remeining = data[1];

            chunks.put(channel, remeining);
        }

        buffer.clear(); //non si sà mai..
        return message;
    }

    /**
     * Setta questo Receiver in modalità dormiente.<br>
     * Un canale in modalità dormiente non riceve nulla e non produce errori se viene invocato
     * qualunque suo metodo receiver<br>
     * <br>
     * Questo metodo ha lo stesso effetto di:
     * setChannel(null).
     */
    @Override
    public void setSleepy() {
        setChannel(null);
    }

    /**
     * Metodo per vedere se un canale è in modalità dormiente.<br>
     * E' consigliato l uso combinato con i valori di ritorno delle funzioni di receive.
     *
     * @return true se il canale è in modalità dormiente, false altrimenti.
     */
    public boolean isSleepy() {
        return channel == null;
    }

    /**
     * Metodo per cambiare canale di ascolto.<br>
     * Successive chiamate a readLine(), vengono eseguite su channel<br>
     */
    public void setChannel(SocketChannel channel) {
        this.channel = channel;
    }

    /**
     * Permette di ottenere il canale su cui questo {@link ChannelReceiver} sta ascoltando<br>
     *
     * @return il canale di ascolto o null se questo Receiver non ha un canale settato<br>
     */
    public SocketChannel getListeningChannel() {
        return channel;
    }

    /**
     * Riceve l intero inviato mediante il metodo {@link ChannelSender#sendInteger(int)} <br>
     * <br>
     *
     * @return l intero ricevuto o -1 se il canale è dormiente. vedi {@link #isSleepy()}
     * @throws EOFException          se viene raggiunto EOF<br>
     * @throws NumberFormatException se la stringa ricevuta non può essere convertita in intero<br>
     * @throws IOException           se avviene qualche errore di I/O<br>
     */
    public int receiveInteger() throws IOException, NumberFormatException {
        if (channel == null) return -1;

        String s = receiveLine();
        if (s == null) throw new EOFException("End of stream reached");

        int i;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Impossibile convertire in numero: " + s);
        }
        return i;
    }

    /**
     * Rimuove un canale dall ascolto e ritorna i dati precedentemente letti.<br>
     *
     * @param channel il canale di ascolto da rimuovere
     * @return quello che è stato letto fin'ora su quel canale o null se è stato letto tutto.<br>
     */
    public String removeListeningChannel(SocketChannel channel) {
        if (channel == null) throw new NullPointerException("Null non è un canale");

        return chunks.remove(channel);
    }

    /**
     * Riceve e decodifica l oggetto inviato mediante {@link ChannelSender#sendObject(Object)}<br>
     * <br>
     *
     * @return l oggetto ricevuto o null se la lettura sul canale fallisce o il canalè in modalità dormiente. vedi {@link #isSleepy()}<br>
     * @throws IOException se avviene qualche errore da I/O<br>
     */
    public Object receiveObject() throws IOException {
        if (channel == null) return null;

        try {
            String serial = receiveLine();
            if (serial == null) return null;

            byte[] receivedSerial = Base64.getDecoder().decode(serial.getBytes());
            ByteArrayInputStream in = new ByteArrayInputStream(receivedSerial);
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(in));
            Object obj = ois.readObject();
            ois.close();

            return obj;
        } catch (EOFException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
