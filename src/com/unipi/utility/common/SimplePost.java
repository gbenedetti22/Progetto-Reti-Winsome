package com.unipi.utility.common;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class SimplePost implements Comparable<SimplePost>, Serializable {
    private String id;
    private String author;
    private String title;
    private String content;
    private Date date;
    private String rewin;

    public SimplePost(String id, String author, String title, String content, Date date) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.content = content;
        this.date = date;
        this.rewin = null;

        int TITLE_MAX_LENGHT = 15;
        if (title.length() > TITLE_MAX_LENGHT) {
            this.title = title.substring(0, TITLE_MAX_LENGHT).concat("...");
        }

        int CONTENT_MAX_LENGHT = 100;
        if (title.length() > CONTENT_MAX_LENGHT) {
            this.title = title.substring(0, CONTENT_MAX_LENGHT).concat("...");
        }
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

    public Date getDate() {
        return date;
    }

    public String getRewin() {
        return rewin;
    }

    public void setRewin(String rewin) {
        this.rewin = rewin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimplePost that = (SimplePost) o;
        return id.equals(that.id) && rewin.equals(that.rewin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, date, rewin);
    }

    @Override
    public int compareTo(SimplePost o) {
        int comparison = this.date.compareTo(o.getDate());

        return comparison == 0 ? 1 : comparison;
    }

    @Override
    public String toString() {
        return "SimplePost{" +
                "author='" + author + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                (rewin != null ? ", rewin='" + rewin + '\'' : "NOT REWINNED") +
                '}';
    }
}
