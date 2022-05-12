package winsome.server.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

/*
    Vedi metodo ServerMain.register() per vedere come si viene registrati al servizio di follow
 */
public interface FollowersDatabase extends Remote {

    // vedi -> ServerRequestReader.performFollow()
    void addFollower(String username) throws RemoteException;

    // vedi -> ServerRequestReader.performUnfollow()
    void removeFollower(String username) throws RemoteException;

    // vedi -> ServerRequestReader.performGetFollowers()
    void setFollowers(Set<String> followers) throws RemoteException;
}
