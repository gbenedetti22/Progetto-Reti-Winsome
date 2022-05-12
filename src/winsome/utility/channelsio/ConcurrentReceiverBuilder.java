package winsome.utility.channelsio;

import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Classe che permette la creazione di un nuovo {@link ConcurrentChannelLineReceiver}.<br>
 * <br>
 * Mediante questa classe è possbile assegnare una propria chunksMap.
 */
public class ConcurrentReceiverBuilder {
    private static final ConcurrentHashMap<SocketChannel, ReentrantLock> locksMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<SocketChannel, String> chunksMap = new ConcurrentHashMap<>();

    /**
     * Metodo che permette di settare una propria chunksMap<br>
     * <br>
     *
     * @param chunksMap la nuova chunksMap
     * @throws NullPointerException se chunksMap è null
     */
    public static void setChunksMap(ConcurrentHashMap<SocketChannel, String> chunksMap) throws NullPointerException {
        if (chunksMap == null) throw new NullPointerException("la chunksMap non può essere null");

        ConcurrentReceiverBuilder.chunksMap = chunksMap;
    }

    /**
     * Crea un nuovo {@link ConcurrentChannelLineReceiver}
     *
     * @return un nuovo ConcurrentChannelLineReceiver
     */
    protected static ConcurrentChannelLineReceiver newConcurrentReceiver() {
        return new ConcurrentChannelLineReceiver(chunksMap, locksMap);
    }
}
