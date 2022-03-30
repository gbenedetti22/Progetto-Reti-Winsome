package com.unipi.database.utility;

import com.unipi.common.HeaderPost;
import com.unipi.database.Database;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class EntriesStorage {
    private HashMap<UUID, Entry> map;
    private Database database;

    public EntriesStorage(Database database) {
        this.database = database;
        this.map = new HashMap<>();
    }

    public void add(Like l) {
        if (l.getType() == Like.type.LIKE) {
            addLike(l);
        } else {
            addDislike(l);
        }
    }

    private synchronized void addLike(Like l) {
        if (map.containsKey(l.getIdPost())) {
            map.get(l.getIdPost()).LIKES.add(l);
            map.get(l.getIdPost()).HEADER.addCurator(l.getUsername());
            return;
        }

        Post post = database.getPost(l.getIdPost());
        HeaderPost headerPost = new HeaderPost(l.getIdPost(), post.getAuthor(), post.getInteractions());
        HashSet<Like> likes = new HashSet<>();
        likes.add(l);

        map.put(l.getIdPost(), new Entry(headerPost, likes, new HashSet<>(), new HashMap<>()));
    }

    private synchronized void addDislike(Like l) {
        if (map.containsKey(l.getIdPost())) {
            map.get(l.getIdPost()).DISLIKES.add(l);
            map.get(l.getIdPost()).HEADER.addCurator(l.getUsername());
            return;
        }

        Post post = database.getPost(l.getIdPost());
        HeaderPost headerPost = new HeaderPost(l.getIdPost(), post.getAuthor(), post.getInteractions());
        HashSet<Like> dislikes = new HashSet<>();
        dislikes.add(l);

        map.put(l.getIdPost(), new Entry(headerPost, new HashSet<>(), dislikes, new HashMap<>()));
    }

    public synchronized void add(Comment c) {
        if (map.containsKey(c.getIdPost())) {
            map.get(c.getIdPost()).COMMENTS.putIfAbsent(c.getAuthor(), new ArrayList<>());
            map.get(c.getIdPost()).COMMENTS.get(c.getAuthor()).add(c);
            map.get(c.getIdPost()).HEADER.addCurator(c.getAuthor());
            return;
        }

        Post post = database.getPost(c.getIdPost());
        HeaderPost headerPost = new HeaderPost(c.getIdPost(), post.getAuthor(), post.getInteractions());

        HashMap<String, ArrayList<Comment>> comments = new HashMap<>();
        comments.put(c.getAuthor(), new ArrayList<>());
        comments.get(c.getAuthor()).add(c);

        map.put(c.getIdPost(), new Entry(headerPost, new HashSet<>(), new HashSet<>(), comments));
    }

    public synchronized ArrayList<Entry> pull() {
        ArrayList<Entry> array = new ArrayList<>(map.values());

        map.clear();

        return array;
    }

    public void changeLikeToDislike(Like l) {
        if (map.containsKey(l.getIdPost())) {
            if (!map.get(l.getIdPost()).LIKES.remove(l)) {
                System.out.println("Errore: Tentato cambio di like a dislike");
                return;
            }

            map.get(l.getIdPost()).DISLIKES.add(l);
            return;
        }

        System.out.println("Errore: Tentato cambio di like a dislike");
    }

    public void chageDislikeToLike(Like l) {
        if (map.containsKey(l.getIdPost())) {
            if (!map.get(l.getIdPost()).DISLIKES.remove(l)) {
                System.out.println("Errore: Tentato cambio di dislike a like");
                return;
            }

            map.get(l.getIdPost()).LIKES.add(l);
            return;
        }

        System.out.println("Errore: Tentato cambio di dislike a like");
    }

    public void remove(UUID id) {
        map.remove(id);
    }


    public static class Entry implements Serializable {
        public final HeaderPost HEADER;
        public final HashSet<Like> LIKES;
        public final HashSet<Like> DISLIKES;
        public final HashMap<String, ArrayList<Comment>> COMMENTS;

        public Entry(HeaderPost HEADER, HashSet<Like> LIKES, HashSet<Like> DISLIKES, HashMap<String, ArrayList<Comment>> COMMENTS) {
            this.HEADER = HEADER;
            this.LIKES = LIKES;
            this.DISLIKES = DISLIKES;
            this.COMMENTS = COMMENTS;
        }

        @Override
        public String toString() {
            return String.format("IdPost: %s, LIKES: %d, DISLIKES: %d, Numero di Persone che hanno commentato: %d", HEADER.getIdPost(), LIKES.size(), DISLIKES.size(), COMMENTS.size());
        }
    }
}