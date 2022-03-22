package com.unipi.client;

import com.unipi.common.Console;
import com.unipi.server.RMI.FollowersDatabase;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Set;

public class LocalStorage extends UnicastRemoteObject implements FollowersDatabase, Serializable {
    private ArrayList<String> followers;
    private ArrayList<String> following;
    private String currentUsername;

    public LocalStorage() throws RemoteException {
        super();
        this.followers = new ArrayList<>();
        this.following = new ArrayList<>();
        this.currentUsername = null;
    }

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

    public ArrayList<String> getFollowing() {
        return following;
    }

    public void setFollowing(ArrayList<String> following) {
        this.following = following;
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    @Override
    public void setFollowers(Set<String> followers) throws RemoteException {
        this.followers.addAll(followers);
        Console.log("Followers settati", this.followers);
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void clear() {
        following.clear();
        followers.clear();
        this.currentUsername = null;
    }
}
