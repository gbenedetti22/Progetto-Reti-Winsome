package com.unipi.database.tables;

import java.util.Objects;
import java.util.UUID;

public class Like {
    public enum type {
        LIKE,
        DISLIKE
    }

    private final UUID idLike;
    private final UUID idPost;
    private final type type;
    private transient long linePosition;

    public Like(UUID idPost, Like.type type) {
        this.idPost = idPost;
        this.type = type;

        this.idLike = UUID.randomUUID();
        linePosition = -1;
    }

    public UUID getIdPost() {
        return idPost;
    }

    public Like.type getType() {
        return type;
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
        return Objects.hash(idLike, idPost, type);
    }
}
