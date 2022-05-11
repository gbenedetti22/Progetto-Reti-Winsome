package com.unipi.common;

import java.io.Serializable;
import java.util.HashSet;
import java.util.UUID;

/*
    Classe che rappresenta l header di una entri.
    Contiene tutte le informazioni di un Post, necessarie al calcolo delle ricompense (autore, curatori ecc)

    Viene creata nell EntriesStorage e viene letta, dal Server, nel RewardCalculator
 */
public class HeaderPost implements Serializable {
    private UUID idPost;
    private String author;
    private HashSet<String> curatori;
    private int interactions;

    public HeaderPost(UUID idPost, String author, int interactions) {
        this.idPost = idPost;
        this.author = author;
        this.interactions = interactions;
        this.curatori = new HashSet<>();
    }

    public UUID getIdPost() {
        return idPost;
    }

    public String getAuthor() {
        return author;
    }

    public int getInteractions() {
        return interactions;
    }

    public void addCurator(String curator) {
        this.curatori.add(curator);
    }

    public HashSet<String> getCurators() {
        return curatori;
    }

    @Override
    public String toString() {
        return "HeaderPost{" +
                "author='" + author + '\'' +
                '}';
    }
}
