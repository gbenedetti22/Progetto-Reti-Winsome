package com.unipi.database.tables;

import com.unipi.common.SimplePost;
import com.unipi.database.graph.graphNodes.GroupNode;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class Post implements Comparable<Post>, Serializable {
    public static class DatePair implements Serializable{
        private Date dateD;
        private String dateS;

        public DatePair() {
            dateD = new Date();
            dateS = new SimpleDateFormat("dd/MM/yy - hh:mm:ss").format(dateD);
        }

        @Override
        public String toString(){
            return dateS;
        }

        public Date toDate(){
            return dateD;
        }
    }

    private final UUID id;
    private String author;
    private String title;
    private String content;
    private DatePair date;
    private transient GroupNode comments;
    private transient GroupNode likes;
    private transient long linePosition;

    public Post(String author, String title, String content) {
        this.author = author;
        this.title = title;
        this.content = content;

        id = UUID.randomUUID();
        date = new DatePair();
        linePosition = -1;
    }

    public UUID getId() {
        return id;
    }

    public String getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public DatePair date(){
        return date;
    }

    public GroupNode getCommentsGroupNode() {
        return comments;
    }

    public void setCommentsGroupNode(GroupNode comments) {
        this.comments = comments;
    }

    public GroupNode getLikesGroupNode() {
        return likes;
    }

    public void setLikesGroupNode(GroupNode likes) {
        this.likes = likes;
    }

    public long getLinePosition() {
        return linePosition;
    }

    public void setLinePosition(long linePosition) {
        this.linePosition = linePosition;
    }

    @Override
    public int compareTo(Post p) {
        if(p.getId().equals(id)) return 0;

        Date date1 = date.dateD;
        Date date2 = p.date.dateD;
        if(date1.after(date2)) return 1;
        else return -1;
    }

    @Override
    public String toString() {
        return "Post {" +
                "author='" + author + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return getId().equals(post.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, author, title, content);
    }

    public SimplePost toSimplePost() {
        return new SimplePost(id.toString(), author, title, content, date.toString());
    }
}
