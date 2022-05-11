package com.unipi.database.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.unipi.database.Database;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.UUID;

/*
    Oggetto che permette la ricreazione del grafo al riavvio del database.
    La ricreazione avviene con le seguenti priorità:
    1 utente
    2 post
    3 commenti
    4 like
    5 rewins
    Nota: con "se l elemento è presente nel db" si intende "se l elemento è presente nelle tabelle tableUsers o tablePosts".
 */
public class GraphLoader {
    private Gson gson;

    // il grafo si basa sulle 2 tabelle (tableUsers e tablePosts)
    // Se un elemento non sta nel db, allora non viene caricato e viene eliminato
    private Database db;
    private WinsomeGraph graph;

    public GraphLoader(Database db, WinsomeGraph graph) {
        this.db = db;
        this.graph = graph;

        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(Database.getDateFormat().toString()).create();
    }

    // metodo per ricreazre il grafo leggendo i file sul disco
    public void loadGraph() throws IOException {
        File dbFolder = new File(Database.getName());

        // considero tutti i file che non sono il rewins file e le directory
        File[] users = dbFolder.listFiles(f -> (!f.getName().equals("rewins") && !f.isDirectory()));

        if (users != null) {
            for (File file : users) {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                String username = file.getName();
                if (!loadUser(username)) continue;

                long linePosition = 0; // contiene la posizione della riga corrente (partendo dalla prima lettera)
                String line;
                while ((line = raf.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        String[] lineSplitted = line.split(";", 2);
                        boolean loaded = true;

                        switch (lineSplitted[0]) {
                            case "POST" -> loaded = loadPost(username, lineSplitted[1], linePosition);
                            case "COMMENT" -> loaded = loadComment(username, lineSplitted[1], linePosition);
                            case "LIKE" -> loaded = loadLike(username, lineSplitted[1], linePosition);
                        }

                        // Se non ho messo nel grafo la entry corrente, la rimuovo dal file con #
                        if (!loaded) {
                            raf.seek(linePosition);
                            raf.writeBytes("#".repeat(line.length()));
                        }
                    }

                    linePosition = raf.getFilePointer();
                }
                raf.close();
            }
        }

        loadRewins();
    }

    // carico un utente sse è presente nel db
    // Viene creato un nodo utente, i relativi tagsGroup e postsGroup e i tag vengono appesi al nodo tagsGroup
    private boolean loadUser(String username) {
        User u = db.getUser(username);
        if (u == null) return false;

        GraphNode<String> node = new GraphNode<>(username);

        String TAGS_LABEL = "TAGS";
        GroupNode tagsGroup = new GroupNode(TAGS_LABEL, node);

        String POSTS_LABEL = "POSTS";
        GroupNode postsGroup = new GroupNode(POSTS_LABEL, node);

        graph.putEdge(node, tagsGroup);
        graph.putEdge(node, postsGroup);

        u.setPostsGroupNode(postsGroup);
        u.setTagsGroupNode(tagsGroup);

        for (String tag : u.getTags()) {
            GraphNode<String> tagNode = new GraphNode<>(tag);
            graph.putEdge(tagsGroup, tagNode);
        }

        return true;
    }

    // viene caricato un post sse è presente nel db
    // Viene creato un nodo post, i relativi likesGroup e commentsGroup
    private boolean loadPost(String username, String id, long linePosition) {
        UUID idPost = UUID.fromString(id);
        Post p = db.getPost(idPost);
        User u = db.getUser(username);

        if (p != null && u != null) {
            p.setLinePosition(linePosition);

            GroupNode postsGroup = u.getPostsGroupNode();

            GraphNode<UUID> postNode = new GraphNode<>(p.getId());
            graph.putEdge(postsGroup, postNode);

            String COMMENTS_LABEL = "COMMENTS";
            GroupNode comments = new GroupNode(COMMENTS_LABEL, postNode);

            String LIKES_LABEL = "LIKES";
            GroupNode likes = new GroupNode(LIKES_LABEL, postNode);

            graph.putEdge(postNode, comments);
            graph.putEdge(postNode, likes);

            p.setCommentsGroupNode(comments);
            p.setLikesGroupNode(likes);

            return true;
        }

        return false;
    }

    // metodo per caricare un singolo commento.
    // Il commento viene caricato sse esiste un file json nella cartella jsons che come nome ha l'id del commento
    // Se il commento esiste, allora viene caricato dal json e appeso al post di riferimento
    private boolean loadComment(String username, String record, long linePosition) {
        try {
            String[] splittedRecord = record.split(";", 2);
            String idComment = splittedRecord[0];
            String newEntryLabel;
            try {
                newEntryLabel = splittedRecord[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                newEntryLabel = "";
            }

            String path = jsonPathOf(idComment);
            if (!Files.exists(Paths.get(path), LinkOption.NOFOLLOW_LINKS)) return false;

            JsonReader reader = new JsonReader(new FileReader(path));
            Comment c = gson.fromJson(reader, Comment.class);
            reader.close();

            if (c == null) return false;

            Post p = db.getPost(c.getIdPost());
            User u = db.getUser(username);
            if (p != null && u != null) {
                c.setLinePosition(linePosition);

                GraphNode<Comment> commentNode = new GraphNode<>(c);
                GroupNode commentsGroup = p.getCommentsGroupNode();

                graph.putEdge(commentsGroup, commentNode);
                // Se il commento era nuovo, allora lo aggiungi all entries storage per il calcolo delle ricompense
                if (!newEntryLabel.startsWith("#") && !newEntryLabel.isBlank()) {
                    db.getEntriesStorage().add(c);
                }
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    //UUID;NEW_NETRY || UUID;#########
    private boolean loadLike(String username, String record, long linePosition) {
        try {
            String[] splittedRecord = record.split(";", 2);
            String idLike = splittedRecord[0];
            String newEntryLabel;
            try {
                newEntryLabel = splittedRecord[1];
            } catch (ArrayIndexOutOfBoundsException e) {
                newEntryLabel = "";
            }

            String path = jsonPathOf(idLike);
            if (!Files.exists(Paths.get(path), LinkOption.NOFOLLOW_LINKS)) return false;

            JsonReader reader = new JsonReader(new FileReader(path));
            Like l = gson.fromJson(reader, Like.class);
            reader.close();

            if (l == null) return false;

            Post p = db.getPost(l.getIdPost());
            User u = db.getUser(username);
            if (p != null && u != null) {
                l.setLinePosition(linePosition);

                GraphNode<Like> likeNode = new GraphNode<>(l);
                GroupNode likesGroup = p.getLikesGroupNode();

                graph.putEdge(likesGroup, likeNode);
                if (!newEntryLabel.startsWith("#") && !newEntryLabel.isBlank()) {
                    db.getEntriesStorage().add(l);
                }

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    // metodo per ricreare i rewins.
    // un rewin esiste se esite il post originale nel db. In tal caso viene ricreato il collegamento.
    private void loadRewins() {
        String rewinsFile = Database.getName() + File.separator + "rewins";

        File f = new File(rewinsFile);
        if (!f.exists()) return;

        try {
            RandomAccessFile in = new RandomAccessFile(f, "rw");
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#"))
                    continue; // questo raramente è vero, ma con un implementazione più efficiente, potrebbe essere utile

                String[] record = line.split(";", 2);
                String username = record[0];
                UUID idPost = UUID.fromString(record[1]);

                User u = db.getUser(username);
                Post p = db.getPost(idPost);
                if (u == null || p == null) {
                    in.seek(Math.max(in.getFilePointer() - line.length() - 2, 0)); // torno all inizio della riga
                    in.writeBytes("#".repeat(line.length()));
                    continue;
                }

                GroupNode posts = u.getPostsGroupNode();
                GraphNode<UUID> node = new GraphNode<>(idPost);
                graph.putEdge(posts, node);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metodo che restituisce il path del file json nella cartella jsons
    private String jsonPathOf(String filename) {
        return Database.getName() + File.separator + "jsons" + File.separator + filename + ".json";
    }
}
