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
    private HashMap<UUID, Entry> map;   //tabella con tutte le entry
    private Database database;

    public EntriesStorage(Database database) {
        this.database = database;
        this.map = new HashMap<>();
    }

    public void add(Like l) {
        if (l.getType() == Like.TYPE.LIKE) {
            addLike(l);
        } else {
            addDislike(l);
        }
    }

    // metodo che aggiunge un like all entries storage
    // Se nell entries storage è già presente il post, viene aggiunto un like nel suo Set
    // altrimenti viene creata ex-nova la entry
    private synchronized void addLike(Like l) {
        // Se il post già sta nell entries storage, aggiungo il like al Set
        if (map.containsKey(l.getIdPost())) {
            map.get(l.getIdPost()).LIKES.add(l);
            map.get(l.getIdPost()).HEADER.addCurator(l.getUsername());
            return;
        }

        // Se il post non esiste, allora lo aggiungo come entry e poi metto il like nel Set
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

    // metodo per aggiungere un commento all entries storage
    // Il procedimento è simile a quello adottato per i like,
    // cambia solo che se l autore del commento non è presente nella tabella dei comments, viene aggiunto
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

    // metodo per cambiare da like a dislike
    // Il vecchio like viene trasformato in dislike spostandolo nel Set corrispondente
    public void changeLikeToDislike(Like l) {
        if (map.containsKey(l.getIdPost())) {
            if (!map.get(l.getIdPost()).LIKES.remove(l)) {
                System.out.println("Errore nel tentativo di cambio da like a dislike");
                return;
            }

            map.get(l.getIdPost()).DISLIKES.add(l);
            return;
        }

        System.out.println("Errore: Tentato cambio di like a dislike su un post inesistente");
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


    // Classe di appoggio che rappresenta una Entry nell entry storage
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