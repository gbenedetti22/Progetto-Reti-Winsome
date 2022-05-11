package com.unipi.common;

import java.util.ArrayList;

/*
    Classe che viene usata come risposta al Login.
    Contiene i tags dell utente e l indirizzo/porta di multicast
 */
public class LoginResponse {
    private ArrayList<String> tags;
    private String multicastAddress;
    private int multicastPort;

    public LoginResponse(ArrayList<String> tags, String multicastAddress, int multicastPort) {
        this.tags = tags;
        this.multicastAddress = multicastAddress;
        this.multicastPort = multicastPort;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public String getMulticastAddress() {
        return multicastAddress;
    }

    public int getMulticastPort() {
        return multicastPort;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "tags=" + tags +
                ", multicastAddress='" + multicastAddress + '\'' +
                ", multicastPort=" + multicastPort +
                '}';
    }
}
