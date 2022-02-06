package com.unipi.database.graph;

import com.google.common.graph.EndpointPair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.WinsomeDatabase;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WinsomeGraph {
    private MutableGraph<Node> graph;
    private ReentrantReadWriteLock rwlock;
    private WinsomeDatabase db;
    private Gson gson;
    private GroupNode newEntryGroup;
    private GraphLoader loader;

    public WinsomeGraph(WinsomeDatabase db) {
        graph = GraphBuilder.undirected().allowsSelfLoops(false).build();
        rwlock = new ReentrantReadWriteLock();
        newEntryGroup = new GroupNode("NEW ENTRY", null);
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat("dd/MM/yy - hh:mm:ss").create();
        this.db = db;

        File dbFolder = new File("graphDB");
        if (!dbFolder.exists()) {
            if (!dbFolder.mkdir()) {
                System.err.println("Errore nella creazione della cartella del grafo");
                System.exit(-1);
            }
        }
        loader = new GraphLoader(db, this);
        try {
            loader.loadGraph();
//            loadGraph();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addNode(Node n) {
        rwlock.writeLock().lock();
        boolean inserted = graph.addNode(n);
        rwlock.writeLock().unlock();

        return inserted;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean putEdge(Node n1, Node n2) {
        rwlock.writeLock().lock();
        boolean inserted = graph.putEdge(n1, n2);
        rwlock.writeLock().unlock();

        return inserted;
    }

    public Set<Node> adjacentNodes(Node n) {
        rwlock.readLock().lock();
        Set<Node> set = graph.adjacentNodes(n);
        rwlock.readLock().unlock();

        return set;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeNode(Node n) {
        rwlock.writeLock().lock();
        boolean deleted = graph.removeNode(n);
        rwlock.writeLock().unlock();

        return deleted;
    }

    public Set<Node> nodes() {
        rwlock.readLock().lock();
        Set<Node> set = graph.nodes();
        rwlock.readLock().unlock();

        return set;
    }

    public Set<EndpointPair<Node>> edges() {
        rwlock.readLock().lock();
        Set<EndpointPair<Node>> set = graph.edges();
        rwlock.readLock().unlock();

        return set;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean removeEdge(Node n1, Node n2) {
        rwlock.writeLock().lock();
        boolean deleted = graph.removeEdge(n1, n2);
        rwlock.writeLock().unlock();

        return deleted;
    }

    public void clearEdges(Node n){
        rwlock.writeLock().lock();
        graph.removeNode(newEntryGroup);
        graph.addNode(newEntryGroup);
        rwlock.writeLock().unlock();
    }

//    public void saveUser(String username) {
//        if (username == null) return;
//
//        try {
//            File file = new File(pathof(username));
//            if (!file.exists()) {
//                if (!file.createNewFile()) {
//                    System.err.println("Impossibile salvare l utente: " + username);
//                    System.err.println("Non è stato possibile creare il file");
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void savePost(Post p) {
//        if (p == null || p.getLinePosition() != 0) return;
//
//        try {
//            RandomAccessFile file = new RandomAccessFile(pathof(p.getAuthor()), "rw");
//            file.seek(file.length());
//            p.setLinePosition(file.getFilePointer());
//
//            file.writeBytes(String.format("POST;%s\n", p.getId().toString()));
//            file.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void removePost(Post p, Set<Node> comments, Set<Node> likes) {
//        if (p == null) return;
//
//        String username = p.getAuthor();
//        try {
//            RandomAccessFile raf = new RandomAccessFile(pathof(username), "rw");
//            raf.seek(p.getLinePosition());
//            raf.writeBytes("#".repeat(36 + 5)); //lunghezza "POST;" + lunghezza UUID
//
//            for (Node n : comments) {
//                if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
//                    long linePosition = c.getLinePosition();
//                    raf.seek(linePosition);
//                    raf.writeBytes("#".repeat(8 + 36)); //COMMENT; + UUID
//
//                    File file = new File(jsonPathOf(c.getId().toString()));
//                    if (!file.delete()) {
//                        System.err.println("Errore nel cancellare il file " + file.getName());
//                        file.deleteOnExit();
//                    }
//                }
//            }
//
//            for (Node n : likes) {
//                if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
//                    long linePosition = l.getLinePosition();
//                    raf.seek(linePosition);
//                    raf.writeBytes("#".repeat(5 + 36)); //LIKE; + UUID
//
//                    File file = new File(jsonPathOf(l.getId().toString()));
//                    if (!file.delete()) {
//                        System.err.println("Errore nel cancellare il file " + file.getName());
//                        file.deleteOnExit();
//                    }
//
//                }
//            }
//
//            raf.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public void saveComment(String username, Comment c) {
//        if (username == null || c == null || c.getLinePosition() != 0) return;
//
//        try {
//            RandomAccessFile file = new RandomAccessFile(pathof(username), "rw");
//            file.seek(file.length());
//            c.setLinePosition(file.getFilePointer());
//
//            file.writeBytes(String.format("COMMENT;%s;NEW_ENTRY\n", c.getId()));
//            file.close();
//
//            String s = gson.toJson(c);
//            BufferedWriter out = new BufferedWriter(new PrintWriter(jsonPathOf(c.getId().toString())));
//            out.write(s);
//            out.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void saveLike(String username, Like l) {
//        if (l == null) return;
//
//        try {
//            RandomAccessFile file = new RandomAccessFile(pathof(username), "rw");
//            file.seek(file.length());
//            l.setLinePosition(file.getFilePointer());
//
//            file.writeBytes(String.format("LIKE;%s;NEW_ENTRY\n", l.getId()));
//            file.close();
//
//            String s = gson.toJson(l);
//            BufferedWriter out = new BufferedWriter(new PrintWriter(jsonPathOf(l.getId().toString())));
//            out.write(s);
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void saveRewin(User u, UUID idPost) {
//        String rewinFile = "graphDB" + File.separator + "rewins";
//
//        try {
//            BufferedWriter out = new BufferedWriter(new FileWriter(rewinFile, true));
//            out.write(String.format("%s;%s\n", u.getUsername(), idPost.toString()));
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void removeEntry(String username, Like l){
//        try {
//            RandomAccessFile file = new RandomAccessFile(pathof(username), "rw");
//            long linePosition = l.getLinePosition();
//
//            file.seek(linePosition + "LIKE".length() + 36 + 2);
//            file.writeBytes("#".repeat("NEW_ENTRY".length()));
//            file.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void removeEntry(String username, Comment c){
//        try {
//            RandomAccessFile file = new RandomAccessFile(pathof(username), "rw");
//            long linePosition = c.getLinePosition();
//
//            file.seek(linePosition + "COMMENT".length() + 36 + 2);
//            file.writeBytes("#".repeat("NEW_ENTRY".length()));
//            file.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void loadGraph() throws IOException {
//        File dbFolder = new File("graphDB");
//        if (!dbFolder.exists()) {
//            if (!dbFolder.mkdir()) {
//                System.err.println("Impossibile creare la cartella del WinsomeDatabase");
//                System.exit(-1);
//            }
//        }
//
//        File[] users = dbFolder.listFiles(f -> (!f.getName().equals("rewins") || !f.getName().equals("new_entries")) && !f.isDirectory());
//        if (users != null) {
//            for (File file : users) {
//                RandomAccessFile raf = new RandomAccessFile(file, "r");
//                String username = file.getName();
//                loadUser(username);
//
//                long linePosition = 0;
//                String line;
//                while ((line = raf.readLine()) != null) {
//                    if (!line.startsWith("#")) {
//                        String[] lineSplitted = line.split(";", 2);
//                        switch (lineSplitted[0]) {
//                            case "POST" -> loadPost(username, lineSplitted[1], linePosition);
//                            case "COMMENT" -> loadComment(username, lineSplitted[1], linePosition);
//                            case "LIKE" -> loadLike(username, lineSplitted[1], linePosition);
//                        }
//                    }
//
//                    linePosition = raf.getFilePointer();
//                }
//                raf.close();
//            }
//        }
//
//        loadRewins();
//
//    }
//
//    private void loadUser(String username) {
//        User u = db.getUser(username);
//        if (u == null) return;
//
//        GraphNode<String> node = new GraphNode<>(username);
//
//        String TAGS_LABEL = "TAGS";
//        GroupNode tagsGroup = new GroupNode(TAGS_LABEL, node);
//
//        String POSTS_LABEL = "POSTS";
//        GroupNode postsGroup = new GroupNode(POSTS_LABEL, node);
//
//        graph.putEdge(node, tagsGroup);
//        graph.putEdge(node, postsGroup);
//
//        u.setPostsGroupNode(postsGroup);
//        u.setTagsGroupNode(tagsGroup);
//
//        for (String tag : u.getTags()) {
//            GraphNode<String> tagNode = new GraphNode<>(tag);
//            graph.putEdge(tagsGroup, tagNode);
//        }
//    }
//
//    private void loadPost(String username, String id, long linePosition) {
//        UUID idPost = UUID.fromString(id);
//        Post p = db.getPost(idPost);
//        User u = db.getUser(username);
//
//        if (p != null && u != null) {
//            p.setLinePosition(linePosition);
//
//            GroupNode postsGroup = u.getPostsGroupNode();
//
//            GraphNode<UUID> postNode = new GraphNode<>(p.getId());
//            graph.putEdge(postsGroup, postNode);
//
//            String COMMENTS_LABEL = "COMMENTS";
//            GroupNode comments = new GroupNode(COMMENTS_LABEL, postNode);
//
//            String LIKES_LABEL = "LIKES";
//            GroupNode likes = new GroupNode(LIKES_LABEL, postNode);
//
//            graph.putEdge(postNode, comments);
//            graph.putEdge(postNode, likes);
//
//            p.setCommentsGroupNode(comments);
//            p.setLikesGroupNode(likes);
//
//            if (p.date().toDate().after(u.getDateOfLastPost()))
//                u.setDateOfLastPost(p.date().toDate());
//        }
//    }
//
//    private void loadComment(String username, String record, long linePosition) {
//        try {
//            String[] splittedRecord = record.split(";", 2);
//            String idComment = splittedRecord[0];
//            String newEntryLabel = splittedRecord[1];
//
//            JsonReader reader = new JsonReader(new FileReader(jsonPathOf(idComment)));
//            Comment c = gson.fromJson(reader, Comment.class);
//            if(c == null) return;
//
//            Post p = db.getPost(c.getIdPost());
//            User u = db.getUser(username);
//            if (p != null && u != null) {
//                c.setLinePosition(linePosition);
//
//                GraphNode<Comment> commentNode = new GraphNode<>(c);
//                GroupNode commentsGroup = p.getCommentsGroupNode();
//
//                graph.putEdge(commentsGroup, commentNode);
//                if(!newEntryLabel.startsWith("#")){
//                    graph.putEdge(newEntryGroup, commentNode);
//                }
//            }
//            reader.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    //UUID;NEW_NETRY || UUID;#########
//    private void loadLike(String username, String record, long linePosition) {
//        try {
//            String[] splittedRecord = record.split(";", 2);
//            String idLike = splittedRecord[0];
//            String newEntryLabel = splittedRecord[1];
//
//            JsonReader reader = new JsonReader(new FileReader(jsonPathOf(idLike)));
//            Like l = gson.fromJson(reader, Like.class);
//            if(l == null) return;
//
//            Post p = db.getPost(l.getIdPost());
//            User u = db.getUser(username);
//            if (p != null && u != null) {
//                l.setLinePosition(linePosition);
//
//                GraphNode<Like> likeNode = new GraphNode<>(l);
//                GroupNode likesGroup = p.getLikesGroupNode();
//
//                graph.putEdge(likesGroup, likeNode);
//                if(!newEntryLabel.startsWith("#")){
//                    graph.putEdge(newEntryGroup, likeNode);
//                }
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    private void loadRewins() {
//        String rewinsFile = "graphDB" + File.separator + "rewins";
//
//        File f = new File(rewinsFile);
//        if (!f.exists()) return;
//
//        try {
//            BufferedReader in = new BufferedReader(new FileReader(f));
//            String line;
//            while ((line = in.readLine()) != null) {
//                String[] record = line.split(";", 2);
//                String username = record[0];
//                UUID idPost = UUID.fromString(record[1]);
//
//                db.rewinFriendsPost(username, idPost);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private String pathof(String username) {
        return "graphDB" + File.separator + username;
    }

    private String jsonPathOf(String filename) {
        return "graphDB" + File.separator + "jsons" + File.separator + filename + ".json";
    }
}
