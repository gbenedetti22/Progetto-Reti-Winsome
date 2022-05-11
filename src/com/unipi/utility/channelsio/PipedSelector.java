package com.unipi.utility.channelsio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Versione della classe {@link Selector} dove l inserimento di un {@link SelectableChannel} può essere accodato.<br>
 * Questo permette che {@link Selector#selectedKeys()} non lanci mai {@link ConcurrentModificationException}
 * e che quindi sia possibile usare un threadpool per soddisfare le richieste.<br>
 * Questa classe si appoggia ad un {@link Selector}, ma <u>NON</u> è una sottoclasse di esso.
 * <br>
 * <u>Nello specifico:</u><br>
 * Per il re-inserimento di una chiave cancellata dal set {@link Selector#selectedKeys()}, viene fatto uso di una queue e di una {@link Pipe}:<br>
 * la chiave viene inserita dentro una queue e poi si procede con la scrittura di 1 byte su una {@link Pipe} registrata all interno del selettore.<br>
 * Quest ultima operazione farà svegliare il Main Thread dalla {@link Selector#select()} e solo allora la chiave verrà re-inserita.<br>
 * Il byte sulla Pipe scritto viene consumato.
 * <p>
 * Esempio d uso tipico:
 * <pre>
 * {@code
 *     public static void main(String[] args) {
 *         try {
 *             ServerSocketChannel server = ServerSocketChannel.open();
 *             server.socket().bind(new InetSocketAddress(port));
 *
 *             selector.insert(server, SelectionKey.OP_ACCEPT);
 *
 *             while (running) {
 *                 selector.selectKey(Main::readSelectedKey);
 *             }
 *
 *         } catch (IOException e) {
 *             e.printStackTrace();
 *         }
 *     }
 *
 *     private static void readSelectedKey(SelectionKey key) {
 *         try {
 *             if (key.isAcceptable()) {
 *                 ServerSocketChannel server = (ServerSocketChannel) key.channel();
 *                 SocketChannel client = server.accept();
 *
 *                 selector.insert(client, SelectionKey.OP_READ);
 *             } else if (key.isReadable()) {
 *                 key.cancel(); // <- molto importante se si vuole usare un threadPool
 *
 *                 SocketChannel client = (SocketChannel) key.channel();
 *                 RequestReader reader = new RequestReader(client);
 *
 *                 threadPool.submit(reader);
 *             } else if (key.isWritable()) {
 *                 key.cancel();
 *
 *                 SocketChannel client = (SocketChannel) key.channel();
 *                 RequestWriter writer = new RequestWriter(client);
 *
 *                 threadPool.submit(writer);
 *             }
 *         } catch (IOException e) {
 *             e.printStackTrace();
 *         }
 *     }
 * }
 * </pre>
 * <p>
 * Per rimettere il Socket dentro il Selector, usare {@link PipedSelector#enqueue(SelectableChannel, int)}
 */
public class PipedSelector implements Closeable {
    private Selector selector;
    private Pipe.SourceChannel pipeIn;
    private Pipe.SinkChannel pipeOut;
    private ByteBuffer inBuffer;
    private ByteBuffer outBuffer;
    private ConcurrentLinkedQueue<KeyEntry> queue;

    /**
     * Costruttore che permette la creazione di un nuovo PipedSelector. <br>
     * In fase di creazione, la Pipe viene aperta sia in scrittura che in lettura
     */
    public PipedSelector() {
        try {
            selector = Selector.open();
            Pipe pipe = Pipe.open();

            pipeIn = pipe.source();
            pipeOut = pipe.sink();
            pipeIn.configureBlocking(false);
            pipeOut.configureBlocking(true);

            pipeIn.register(selector, SelectionKey.OP_READ);
            queue = new ConcurrentLinkedQueue<>();
            inBuffer = ByteBuffer.allocate(1);
            outBuffer = ByteBuffer.wrap(new byte[]{0});
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Crea un nuovo {@link PipedSelector}
     *
     * @return il {@link PipedSelector} creato
     */
    public static PipedSelector open() {
        return new PipedSelector();
    }

    /**
     * <u>Testo copiato da: {@link #insert(SelectableChannel, int, Object)}</u><br>
     * Permette l inserimento diretto senza passare dalla queue.<br>
     * E' fortemente consigliato di usare questa funzione <u>solo</u> nella fase di "preparazione/inizializzazione".<br>
     * Per l inserimento thread-safe vedi {@link #enqueue(SelectableChannel, int, Object)} o {@link #enqueue(SelectableChannel, int)}
     * <br>
     * Questa funzione non è thread-safe, quindi potrebbe far generare {@link ConcurrentModificationException}<br>
     * Il canale inserito viene automaticamente settato come non bloccante mediante la funzione {@link java.nio.channels.SocketChannel#configureBlocking(boolean)}<br>
     *
     * @param channel il canale da inserire
     * @param ops     l operazione che si vuole svolgere
     */
    public void insert(SelectableChannel channel, int ops) {
        try {
            channel.configureBlocking(false);
            channel.register(selector, ops);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Permette l inserimento diretto senza passare dalla queue.<br>
     * E' fortemente consigliato di usare questa funzione <u>solo</u> nella fase di "preparazione/inizializzazione".<br>
     * Per l inserimento thread-safe vedi {@link #enqueue(SelectableChannel, int, Object)} o {@link #enqueue(SelectableChannel, int)}
     * <br>
     * Questa funzione non è thread-safe, quindi potrebbe far generare {@link ConcurrentModificationException}<br>
     * Il canale inserito viene automaticamente settato come non bloccante mediante la funzione {@link java.nio.channels.SocketChannel#configureBlocking(boolean)}<br>
     *
     * @param channel    il canale da inserire
     * @param ops        l operazione che si vuole svolgere
     * @param attachment eventuali dati da associare al canale
     */
    public void insert(SelectableChannel channel, int ops, Object attachment) {
        try {
            channel.configureBlocking(false);
            channel.register(selector, ops, attachment);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * <u>Testo copiato da: {@link #enqueue(SelectableChannel, int, Object)}</u><br>
     * Funzione che permette di accodare channel dentro la queue e di scrivere 1 byte sulla Pipe.<br>
     * Il thread in attesa sulla select verrà svegliato per poter re-inserire channel dentro il selettore.<br>
     * <br>
     * Il canale inserito viene automaticamente settato come non bloccante mediante la funzione {@link java.nio.channels.SocketChannel#configureBlocking(boolean)}<br>
     *
     * @param channel il canale da re-inserire
     * @param ops     l operazione che si vuole svolgere
     */
    public synchronized void enqueue(SelectableChannel channel, int ops) {
        try {
            queue.add(new KeyEntry(channel, ops));

            outBuffer.rewind();
            pipeOut.write(outBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Funzione che permette di accodare channel dentro la queue e di scrivere sulla Pipe.<br>
     * Il thread in attesa sulla select verrà svegliato per poter re-inserire channel dentro il selettore,<br>
     * <br>
     * Il canale inserito viene automaticamente settato come non bloccante mediante la funzione {@link java.nio.channels.SocketChannel#configureBlocking(boolean)}<br>
     *
     * @param channel    il canale da re-inserire
     * @param ops        l operazione che si vuole svolgere
     * @param attachment eventuali dati da associare al canale
     */
    public synchronized void enqueue(SelectableChannel channel, int ops, Object attachment) {
        try {
            queue.add(new KeyEntry(channel, ops, attachment));

            outBuffer.rewind();
            pipeOut.write(outBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Esegue un operazione di {@link Selector#select()} e, per ogni chiave pronta, viene invocata la funzione keyReader,
     * passandogli come parametro la {@link SelectionKey} ottenuta.<br>
     * <br>
     * Il chiamante può invocare {@link SelectionKey#cancel()} per cancellare la chiave dal set {@link Selector#selectedKeys()}
     * e, successivamente, re-inserire la chiave mediante il metodo {@link #enqueue(SelectableChannel, int)} o {@link #enqueue(SelectableChannel, int, Object)}<br>
     * <br>
     *
     * @param keyReader puntatore alla funzione che processa la {@link SelectionKey} passata come parametro
     */
    public void selectKey(Consumer<SelectionKey> keyReader) {
        try {
            int n = selector.select();
            if (n == 0) return;

            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) continue;

                //Leggo 1 byte dalla Pipe e ri-aggiungo il canale
                if (key.isReadable() && key.channel() instanceof Pipe.SourceChannel) {
                    for (int i = 0; i < 1; ) {
                        i += pipeIn.read(inBuffer);
                    }

                    inBuffer.clear();

                    KeyEntry entry = queue.poll();
                    if (entry == null) continue; //di fatto non può succedere, però non si sa mai..

                    SelectableChannel channel = entry.getChannel();
                    int ops = entry.getOps();
                    Object attachment = entry.getAttachment();

                    try {
                        channel.configureBlocking(false);
                        channel.register(selector, ops, attachment);
                    } catch (ClosedChannelException e) {
                        channel.close();
                    }
                } else {
                    keyReader.accept(key);
                }

            }
        } catch (ClosedSelectorException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Chiude il selettore e la Pipe su cui questa classe si appoggia.<br>
     *
     * @throws IOException se avviene qualche errore di I/O durante la chiusura
     */
    @Override
    public void close() throws IOException {
        selector.close();
        pipeOut.close();
        pipeIn.close();
    }

    private static class KeyEntry {
        private SelectableChannel channel;
        private Integer ops;
        private Object attachment;

        public KeyEntry(SelectableChannel channel, Integer ops, Object attachment) {
            this.channel = channel;
            this.ops = ops;
            this.attachment = attachment;
        }

        public KeyEntry(SelectableChannel channel, Integer ops) {
            this.channel = channel;
            this.ops = ops;
        }

        public SelectableChannel getChannel() {
            return channel;
        }

        public Integer getOps() {
            return ops;
        }

        public Object getAttachment() {
            return attachment;
        }
    }
}
