package winsome.server.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FollowersService extends Remote {
    boolean register(String username, FollowersDatabase callback) throws RemoteException;

    boolean unregister(String username) throws RemoteException;
}
