package winsome.database.tables;

import winsome.common.SimpleLike;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Like implements Serializable {
    private final UUID idLike;
    private final UUID idPost;
    private final String username; //chi ha messo il like
    private TYPE type;
    private transient long linePosition;

    public Like(UUID idPost, TYPE type, String username) {
        this.idPost = idPost;
        this.type = type;
        this.username = username;

        this.idLike = UUID.randomUUID();
        linePosition = -1;
    }

    public UUID getIdPost() {
        return idPost;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
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

    public enum TYPE {
        LIKE,
        DISLIKE
    }
}
