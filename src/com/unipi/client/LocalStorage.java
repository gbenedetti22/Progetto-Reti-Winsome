package com.unipi.client;

import com.unipi.server.RMI.FollowersDatabase;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Set;

public class LocalStorage implements FollowersDatabase, Serializable {
    private ArrayList<String> followers = new ArrayList<>();
    private ArrayList<String> following = new ArrayList<>();
    private String currentUsername;

    @Override
    public void addFollower(String username) throws RemoteException {
        followers.add(username);
        System.out.println("Follower: " + username + " aggiuto con successo!");
    }

    @Override
    public void removeFollower(String username) throws RemoteException {
        followers.remove(username);
        System.out.println("Follower: " + username + " rimosso con successo!");
    }

    @Override
    public void setFollowers(Set<String> followers) throws RemoteException {
        this.followers.addAll(followers);
        System.out.println("followers settati!");
    }

    public void setFollowing(ArrayList<String> following) {
        this.following = following;
    }

    public ArrayList<String> getFollowing() {
        return following;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }
}
