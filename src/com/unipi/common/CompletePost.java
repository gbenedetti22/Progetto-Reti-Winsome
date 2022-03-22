package com.unipi.common;

import java.util.Set;

public class CompletePost<C, L> {
    private String id;
    private String author;
    private String title;
    private String content;
    private String date;
    private Set<C> comments;
    private Set<L> likes;

    public CompletePost(String id, String author, String title, String content, String date, Set<C> comments, Set<L> likes) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.content = content;
        this.date = date;
        this.comments = comments;
        this.likes = likes;
    }

    public String getId() {
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

    public String getDate() {
        return date;
    }

    public Set<C> getComments() {
        return comments;
    }

    public void setComments(Set<C> comments) {
        this.comments = comments;
    }

    public Set<L> getLikes() {
        return likes;
    }
}
