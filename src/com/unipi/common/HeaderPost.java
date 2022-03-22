package com.unipi.common;

import java.io.Serializable;
import java.util.UUID;

public class HeaderPost implements Serializable {
    private UUID idPost;
    private String author;
    private int interactions;

    public HeaderPost(UUID idPost, String author, int interactions) {
        this.idPost = idPost;
        this.author = author;
        this.interactions = interactions;
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

    @Override
    public String toString() {
        return "HeaderPost{" +
                "author='" + author + '\'' +
                '}';
    }
}
