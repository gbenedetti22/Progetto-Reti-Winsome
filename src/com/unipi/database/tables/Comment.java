package com.unipi.database.tables;

import com.unipi.common.SimpleComment;
import com.unipi.database.Database;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Comment implements Comparable<Comment>, Serializable {
    private final UUID idComment;
    private final UUID idPost;
    private final String author;
    private final String date;
    private final String content;
    private transient long linePosition;

    public Comment(UUID idPost, String author, String content) {
        this.idPost = idPost;
        this.author = author;
        this.content = content;
        this.date = Database.getDateFormat().getSimpleDateFormat().format(new Date());

        this.idComment = UUID.randomUUID();
        linePosition = -1;
    }

    public UUID getIdPost() {
        return idPost;
    }

    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public UUID getId() {
        return idComment;
    }

    public long getLinePosition() {
        return linePosition;
    }

    public void setLinePosition(long linePosition) {
        this.linePosition = linePosition;
    }

    public SimpleComment toSimpleComment() {
        return new SimpleComment(idComment.toString(), author, content, date);
    }

    @Override
    public String toString() {
        return "Comment {" +
                "idPost=" + idPost +
                ", author='" + author + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Comment comment = (Comment) o;
        return getIdPost().equals(comment.getIdPost()) && getAuthor().equals(comment.getAuthor()) && getDate().equals(comment.getDate()) && getContent().equals(comment.getContent());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idComment, idPost, author, date, content);
    }

    @Override
    public int compareTo(Comment o) {
        try {
            Date d1 = Database.getDateFormat().getSimpleDateFormat().parse(date);
            Date d2 = Database.getDateFormat().getSimpleDateFormat().parse(o.getDate());

            return d1.compareTo(d2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
