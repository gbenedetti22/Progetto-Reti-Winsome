package com.unipi.database;

import com.unipi.common.HeaderPost;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class EntriesStorage {
    private ArrayList<Entry> list;
    private Database database;
    public EntriesStorage(Database database) {
        this.database = database;
        list = new ArrayList<>();
    }

    public synchronized void add(Like l) {
        for (Entry e : list) {
            if (e.HEADER.getIdPost().equals(l.getIdPost())) {
                e.LIKES.add(l);
                return;
            }
        }

        Post post = database.getPost(l.getIdPost());
        HeaderPost headerPost = new HeaderPost(l.getIdPost(), post.getAuthor(), post.getInteractions());
        LinkedList<Like> likes = new LinkedList<>();
        likes.add(l);

        ArrayList<Comment> comments = new ArrayList<>();
        list.add(new Entry(headerPost, likes, comments));
    }

    public synchronized void add(Comment c) {
        for (Entry e : list) {
            if (e.HEADER.getIdPost().equals(c.getIdPost())) {
                partionInsert(e.COMMENTS, c);
                return;
            }
        }

        Post post = database.getPost(c.getIdPost());
        HeaderPost headerPost = new HeaderPost(c.getIdPost(), post.getAuthor(), post.getInteractions());
        LinkedList<Like> likes = new LinkedList<>();
        ArrayList<Comment> comments = new ArrayList<>();
        comments.add(c);
        list.add(new Entry(headerPost, likes, comments));
    }

    private void partionInsert(ArrayList<Comment> list, Comment comment) {
        ListIterator<Comment> iterator = list.listIterator(0);
        while (iterator.hasNext()) {
            Comment c = iterator.next();
            if (c.getAuthor().equals(comment.getAuthor())) {
                iterator.add(comment);
                return;
            }
        }

        list.add(comment);
    }

    public synchronized ArrayList<Entry> pull() {
        ArrayList<Entry> array = new ArrayList<>(list.size());
        array.addAll(list);

        list.clear();

        return array;
    }

    public void print() {
        for (Entry e : list) {
            System.out.println(e.HEADER);

            for (Like l : e.LIKES)
                System.out.print(l.getType() + " ");

            System.out.println();
            for (Comment c : e.COMMENTS)
                System.out.print(c.getAuthor());

            System.out.println();
            System.out.println();
        }
    }

    public static class Entry implements Serializable {
        public final HeaderPost HEADER;
        public final LinkedList<Like> LIKES;
        public final ArrayList<Comment> COMMENTS;

        public Entry(HeaderPost header, LinkedList<Like> likes, ArrayList<Comment> comments) {
            this.HEADER = header;
            this.LIKES = likes;
            this.COMMENTS = comments;
        }
    }
}
