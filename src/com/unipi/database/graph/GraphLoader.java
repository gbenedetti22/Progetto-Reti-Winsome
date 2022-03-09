package com.unipi.database.graph;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.Database;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.UUID;

public class GraphLoader {
    private Gson gson;
    private Database db;
    private WinsomeGraph graph;
    private GroupNode newEntryGroup;

    public GraphLoader(Database db, WinsomeGraph graph) {
        this.db = db;
        this.graph = graph;

        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(Database.getDateFormat().toString()).create();
        newEntryGroup = new GroupNode("NEW ENTRY", null);
    }

    public void loadGraph() throws IOException {
        File dbFolder = new File(Database.getName());

        File[] users = dbFolder.listFiles(f -> (!f.getName().equals("rewins") || !f.getName().equals("new_entries")) && !f.isDirectory());
        if (users != null) {
            for (File file : users) {
                RandomAccessFile raf = new RandomAccessFile(file, "rw");
                String username = file.getName();
                if(!loadUser(username)) continue;

                long linePosition = 0;
                String line;
                while ((line = raf.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        String[] lineSplitted = line.split(";", 2);
                        switch (lineSplitted[0]) {
                            case "POST" -> {
                                boolean loaded = loadPost(username, lineSplitted[1], linePosition);
                                if(!loaded){
                                    raf.seek(linePosition);
                                    raf.writeBytes("#".repeat(line.length()));
                                }
                            }
                            case "COMMENT" -> {
                                boolean loaded = loadComment(username, lineSplitted[1], linePosition);
                                if(!loaded){
                                    raf.seek(linePosition);
                                    raf.writeBytes("#".repeat(line.length()));
                                }
                            }
                            case "LIKE" -> {
                                boolean loaded = loadLike(username, lineSplitted[1], linePosition);
                                if(!loaded){
                                    raf.seek(linePosition);
                                    raf.writeBytes("#".repeat(line.length()));
                                }
                            }
                        }
                    }

                    linePosition = raf.getFilePointer();
                }
                raf.close();
            }
        }

        loadRewins();
    }

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

            if (p.date().toDate().after(u.getDateOfLastPost()))
                u.setDateOfLastPost(p.date().toDate());
            return true;
        }

        return false;
    }

    private boolean loadComment(String username, String record, long linePosition) {
        try {
            String[] splittedRecord = record.split(";", 2);
            String idComment = splittedRecord[0];
            String newEntryLabel = splittedRecord[1];

            String path = jsonPathOf(idComment);
            if(!Files.exists(Paths.get(path), LinkOption.NOFOLLOW_LINKS)) return false;

            JsonReader reader = new JsonReader(new FileReader(path));
            Comment c = gson.fromJson(reader, Comment.class);
            reader.close();

            if(c == null) return false;

            Post p = db.getPost(c.getIdPost());
            User u = db.getUser(username);
            if (p != null && u != null) {
                c.setLinePosition(linePosition);

                GraphNode<Comment> commentNode = new GraphNode<>(c);
                GroupNode commentsGroup = p.getCommentsGroupNode();

                graph.putEdge(commentsGroup, commentNode);
                if(!newEntryLabel.startsWith("#")){
                    graph.putEdge(newEntryGroup, commentNode);
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
            String newEntryLabel = splittedRecord[1];

            String path = jsonPathOf(idLike);
            if(!Files.exists(Paths.get(path), LinkOption.NOFOLLOW_LINKS)) return false;

            JsonReader reader = new JsonReader(new FileReader(path));
            Like l = gson.fromJson(reader, Like.class);
            reader.close();

            if(l == null) return false;

            Post p = db.getPost(l.getIdPost());
            User u = db.getUser(username);
            if (p != null && u != null) {
                l.setLinePosition(linePosition);

                GraphNode<Like> likeNode = new GraphNode<>(l);
                GroupNode likesGroup = p.getLikesGroupNode();

                graph.putEdge(likesGroup, likeNode);
                if(!newEntryLabel.startsWith("#")){
                    graph.putEdge(newEntryGroup, likeNode);
                }

                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void loadRewins() {
        String rewinsFile = Database.getName() + File.separator + "rewins";

        File f = new File(rewinsFile);
        if (!f.exists()) return;

        try {
            RandomAccessFile in = new RandomAccessFile(f, "rw");
            String line;
            while ((line = in.readLine()) != null) {
                if(line.startsWith("#")) continue;

                String[] record = line.split(";", 2);
                String username = record[0];
                UUID idPost = UUID.fromString(record[1]);

                User u = db.getUser(username);
                Post p = db.getPost(idPost);
                if(u == null || p == null){
                    in.seek(Math.max(in.getFilePointer() - line.length() - 2, 0));
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

    private String jsonPathOf(String filename) {
        return Database.getName() + File.separator + "jsons" + File.separator + filename + ".json";
    }
}
