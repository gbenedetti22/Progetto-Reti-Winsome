package com.unipi.client.UI.banners;

public class CommentBanner extends PostBanner {

    protected CommentBanner(String id, String author_name, String title, String content, String date, boolean deletable) {
        super(id, author_name, title, content, date, deletable, false);
    }

    public CommentBanner(String author, String content) {
        this("", "", author, content, "", false);
    }
}
