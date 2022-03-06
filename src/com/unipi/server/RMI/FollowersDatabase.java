package com.unipi.server.RMI;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface FollowersDatabase extends Remote {
    void addFollower(String username) throws RemoteException;
    void removeFollower(String username) throws RemoteException;
    void setFollowers(Set<String> followers) throws RemoteException;
}
