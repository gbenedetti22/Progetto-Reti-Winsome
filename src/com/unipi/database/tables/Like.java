package com.unipi.database.tables;

import com.unipi.common.SimpleLike;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Like implements Serializable {
    private final UUID idLike;
    private final UUID idPost;
    private final String username; //chi ha messo il like
    private type type;
    private transient long linePosition;
    public Like(UUID idPost, Like.type type, String username) {
        this.idPost = idPost;
        this.type = type;
        this.username = username;

        this.idLike = UUID.randomUUID();
        linePosition = -1;
    }

    public UUID getIdPost() {
        return idPost;
    }

    public Like.type getType() {
        return type;
    }

    public void setType(Like.type type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public UUID getId() {
        return idLike;
    }

    public long getLinePosition() {
        return linePosition;
    }

    public void setLinePosition(long linePosition) {
        this.linePosition = linePosition;
    }

    public SimpleLike toSimpleLike() {
        return new SimpleLike(type);
    }

    @Override
    public String toString() {
        return "Like {" +
                "idPost=" + idPost +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return idLike.equals(like.idLike);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLike);
    }

    public enum type {
        LIKE,
        DISLIKE
    }
}
