package winsome.utility.channelsio;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Classe che permette l invio dei dati su uno o più canali.<br>
 * E' garantito che tutti i dati inviati mediante il metodo {@link #sendLine(String)} vengano ricevuti,
 * in ordine di invio, dalle corrispettive chiamate del metodo {@link ChannelLineReceiver#receiveLine()}<br>
 * <p><br>
 * Es. {@link ChannelLineReceiver} vs {@link SocketChannel}<br>
 * ChannelSender.sendLine("ciao");<br>
 * ChannelSender.sendLine("mondo");<br>
 * <p><br>
 * ChannelReceiver.readLine(); -> restituirà "ciao"<br>
 * ChannelReceiver.readLine(); -> restituirà "mondo"<br>
 * <p>===========================================================<br>
 * SocketChannel.write("ciao");<br>
 * SocketChannel.write("mondo");<br>
 * <p><br>
 * SocketChannel.read(); -> può restituire "ciao", "ciao mon", "ciao mondo" ecc<br>
 * SocketChannel.read(); -> può generare errori<br>
 * <br>
 * <p>
 * Implementation-notes:<br>
 * Questa classe è stata progettata per funzionare con un solo buffer di qualsiasi capacità.<br>
 * Il buffer viene allocato in modalità diretta (vedi {@link ByteBuffer#allocateDirect(int)} per motivi di performance.<br>
 * Molte soluzioni usano la tecnicha dell "alloco un buffer grande tanto quanto devo ricevere e poi butto tutto":
 * questo comporta un allocazione/deallocazione continua e quindi un overhead da pagare, che può essere molto alto
 * se i dati da ricevere sono molti.<br><br>
 * Facendo uso di un solo buffer, si evita questo overhead ed è possibile usarne solo uno con questi accorgimenti:<br>
 * - tenere traccia di quanto è stato scritto e di quanto manca ancora da scrivere, durante l invio.<br>
 * - non adattare il buffer in base ai byte da inviare, ma adattare i byte in base alla capacità del buffer<br>
 * - rimpiazzare i byte del buffer, scritti con quelli da scrivere<br>
 * <br>
 * Questa classe è null-safe.
 */
public class ChannelLineSender implements ChannelSender {
    private SocketChannel socket;
    private ByteBuffer buffer;

    /**
     * Crea un nuovo {@link ChannelLineSender} che si mette in ascolto sul canale channel
     *
     * @param channel il canale su cui mettersi in ascolto
     */
    public ChannelLineSender(SocketChannel channel) {
        this.socket = channel;

        buffer = ByteBuffer.allocateDirect(512);
    }

    /**
     * Crea un nuovo {@link ChannelLineSender} che non è in ascolto su nessun canale
     */
    public ChannelLineSender() {
        this(null);
    }

    /**
     * Invia del testo sul canale settato mediante {@link #setChannel(SocketChannel)} o su quello passato al costruttore.<br>
     * E' garantito che msg venga inviato tutto, se il canale è pronto per la scrittura.<br>
     *
     * @param msg il messaggio da inviare
     * @throws IOException se avviene qualche errore di I/O
     */
    public void sendLine(String msg) throws IOException {
        if (socket == null) return;

        String message = msg;
        if (!msg.endsWith(System.lineSeparator()))
            message = msg.concat(System.lineSeparator());


        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        int len = buffer.capacity() + 1;    //+1 perchè, se buffer.capacity() < msg.lenght(),
        // alla prima iterazione del for Math.min() deve restituire buffer.capacity()

        if (buffer.capacity() > bytes.length)
            len = bytes.length; //se la grandezza del buffer è maggiore del messaggio che voglio inviare
        //allora Math.min() deve restituire la grandezza del massaggio

        for (int i = 0; i < bytes.length; ) {
            buffer.put(bytes, i, Math.min(buffer.capacity(), len));
            buffer.flip();
            i += socket.write(buffer);

            len = bytes.length - i; //tengo traccia di quanti bytes rimangono da inviare
            buffer.clear();
        }

        buffer.clear();
    }

    /**
     * Metodo per cambiare canale di invio.
     * Successive chiamate a {@link #sendLine(String)}, vengono eseguite su channel.
     *
     * @param channel il canale su cui questo Sender deve ascoltare o null se si vuole che questo canale non mai invii nulla
     */
    public void setChannel(SocketChannel channel) {
        this.socket = channel;
    }

    /**
     * Metodo per inviare un intero sul canale settato da {@link #setChannel(SocketChannel)}.<br>
     * L intero viene prima convertito in stringa e poi inviato mediante {@link #sendLine(String)}.<br>
     * E' possibile usare il metodo {@link ChannelLineReceiver#receiveInteger()} per ricevere il dato come intero
     * o il metodo {@link ChannelLineReceiver#receiveLine()} per riceverlo come stringa.<br>
     *
     * @param i il numero da inviare
     * @throws IOException se si verifica qualche errore di I/O
     */
    public void sendInteger(int i) throws IOException {
        if (socket == null) return;

        sendLine(String.valueOf(i));
    }

    /**
     * Invia un oggetto sul canale settato usando {@link #setChannel(SocketChannel)}.<br>
     * L oggetto viene serializzato, convertito in stringa e inviato usando
     * il metodo {@link #sendLine(String)}
     *
     * @param obj l oggetto che si vuole inviare
     * @throws IOException Se si verifica qualche errore di I/O
     */
    public void sendObject(Object obj) throws IOException {
        if (socket == null) return;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(out));
        oos.writeObject(obj);
        oos.close();

        String serial = Base64.getEncoder().encodeToString(out.toByteArray());
        sendLine(serial);
    }

    /**
     * Metodo per silenziare questo {@link ChannelSender}.<br>
     * Una chiamata a questo metodo ha lo stesso effetto di: <br>
     * setChannel(null)
     * <p>
     * Successive chiamate a qualunque metodo send, non produrranno errori.
     */
    public void setSleepy() {
        setChannel(null);
    }

    @Override
    public void send(String s) throws IOException {
        sendLine(s);
    }
}
