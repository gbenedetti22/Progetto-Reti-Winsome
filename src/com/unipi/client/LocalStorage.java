package com.unipi.client;

import com.unipi.client.UI.pages.FollowersPage;
import com.unipi.common.Console;
import com.unipi.server.RMI.FollowersDatabase;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Set;

/*
    Classe che gestisce i dati dell applicazione.
    Più nello specifico, vengono tenuti i followers (RMI), i following e lo username su cui l utente si è loggato
    Implementa Serializable per via dell RMI
 */
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

        FollowersPage page = Pages.FOLLOW_PAGE;
        if (following.contains(username)) {
            FollowersPage.PageBanner banner = page.newBanner(username, FollowersPage.Type.FOLLOWING);
            page.addLeft(banner);
            return;
        }

        page.appendBanner(username, FollowersPage.Type.FOLLOWER);
    }

    @Override
    public void removeFollower(String username) throws RemoteException {
        followers.remove(username);
        System.out.println("Follower: " + username + " rimosso con successo!");

        Pages.FOLLOW_PAGE.removeFromLeft(username);
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
        FollowersPage page = Pages.FOLLOW_PAGE;
        page.clearLeft();

        for (String follower : this.followers) {
            //se già sto seguendo questo utente.. -> metto unfollow
            if (this.following.contains(follower)) {
                FollowersPage.PageBanner banner = page.newBanner(follower, FollowersPage.Type.FOLLOWING);
                page.addLeft(banner);
                continue;
            }

            //..altrimenti metto possibilità di follow
            page.appendBanner(follower, FollowersPage.Type.FOLLOWER);
        }
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
