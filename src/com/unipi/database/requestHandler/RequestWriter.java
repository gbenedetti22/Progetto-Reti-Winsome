package com.unipi.database.requestHandler;

import com.google.gson.Gson;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.utility.ThreadWorker;
import com.unipi.utility.channelsio.ChannelSender;
import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.utility.common.SimplePost;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class RequestWriter implements Runnable {
    private SocketChannel socket;
    private PipedSelector selector;
    private Packet packet;
    private ChannelSender out;
    private Database database;
    private Gson gson;

    public RequestWriter(SocketChannel socket, PipedSelector selector, Packet p) {
        this.socket = socket;
        this.selector = selector;
        this.packet = p;
        this.gson = new Gson();
        this.database = DatabaseMain.getDatabase();
    }

    @Override
    public void run() {
        if (packet.isEmpty()) {
            selector.enqueue(socket, SelectionKey.OP_READ);
            return;
        }

        ThreadWorker worker = (ThreadWorker) Thread.currentThread();
        this.out = worker.getSender();
        out.setChannel(socket);

        readPacket();

        selector.enqueue(socket, SelectionKey.OP_READ);
    }

    private void readPacket() {
        switch (packet.getFunction()) {
            case DISCOVER -> discover();
            case GET_FOLLOWERS -> getFollowers();
            case GET_FOLLOWING -> getFollowing();
            case FOLLOW -> follow();
            case UNFOLLOW -> unfollow();
            case CREATE_POST -> createPost();
            case CREATE_USER -> createUser();
            case VIEW_POST -> viewPost();
            case GET_ALL_POSTS -> getAllPosts();
            case FRIENDS_POSTS -> friendsPosts();
            case REWIN -> rewin();
            case REMOVE_REWIN -> removeRewin();
            case COMMENT -> comment();
            case LIKE -> like();
            case DISLIKE -> dislike();
            case REMOVE_POST -> removePost();
            case PULL_ENTRIES -> getLatestEntries();
            case CHECK_IF_EXIST -> findUser();
            case GET_LATEST_POST -> getLatestPost();
            default -> {
            }
        }

    }

    private void createUser() {
        sendString();
    }

    @SuppressWarnings("unchecked")
    private void findUser() {
        Set<Node> set = (Set<Node>) packet.getMessage();
        List<String> l = new LinkedList<>();
        for (Node n : set) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof String tag) {
                l.add(tag);
            }
        }

        try {
            out.sendObject(l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discover() {
        try {
            if (packet.getMessage() instanceof Set<?> set) {
                out.sendInteger(set.size());

                for (Object o : set) {
                    if (o instanceof Node n && n instanceof GroupNode g1) {
                        if(g1.getParent() instanceof GraphNode<?> g2 && g2.getValue() instanceof String s) {
                            out.sendLine(s);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFollowers() {
        sendSetOfString();
    }

    private void getFollowing() {
        sendSetOfString();
    }

    private void follow() {
        sendString();
    }

    private void unfollow() {
        sendString();
    }

    private void createPost() {
        Post p = (Post) packet.getMessage();
        SimplePost simplePost = p.toSimplePost();
        try {
            out.sendObject(simplePost);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void viewPost() {
        HashMap<String, Object> map = (HashMap<String, Object>) packet.getMessage();
        try {
            Set<Node> commentNodes = (Set<Node>) map.get("COMMENTS");
            Set<Node> likeNodes = (Set<Node>) map.get("LIKES");

            ArrayList<Comment> comments = new ArrayList<>(commentNodes.size());
            ArrayList<Like> likes = new ArrayList<>(likeNodes.size());

            for (Node n : commentNodes) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                    comments.add(c);
                }
            }

            for (Node n : likeNodes) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                    likes.add(l);
                }
            }

            map.put("COMMENTS", comments);
            map.put("LIKES", likes);

            out.sendObject(map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getAllPosts() {
        try {
            LinkedList<SimplePost> posts = new LinkedList<>();

            if (packet.getMessage() instanceof Set<?> set) {
                for (Object o : set) {
                    if (o instanceof Node n && n instanceof GraphNode<?> g && g.getValue() instanceof UUID id) {
                        Post p = database.getPost(id);
                        posts.add(p.toSimplePost());
                    }
                }
            }

            out.sendObject(posts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void friendsPosts() {
        sendSetOfPosts();
    }

    private void getLatestPost() {
        sendSetOfPosts();
    }

    private void rewin() {
        sendString();
    }

    private void removeRewin() {
        sendString();
    }

    private void comment() {
        sendString();
    }

    private void like() {
        sendString();
    }

    private void dislike() {
        sendString();
    }

    public void removePost() {
        sendString();
    }

    public void getLatestEntries() {
        try {
            if (packet.getMessage() instanceof Set<?> set) {
                out.sendInteger(set.size());

                for (Object obj : set) {
                    if (obj instanceof GraphNode<?> g) {
                        out.sendObject(g.getValue());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSetOfString() {
        try {
            out.sendObject(packet.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendString() {
        String message = (String) packet.getMessage();
        try {
            out.sendLine(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void sendSetOfPosts() {
        Map<String, Set<Node>> map = (Map<String, Set<Node>>) packet.getMessage();
        HashMap<String, LinkedList<SimplePost>> unpackedMap = new HashMap<>(map.size());
//        LinkedList<SimplePost> posts = new LinkedList<>();
//
//        map.forEach((key, value) -> value
//                .stream()
//                .map(n -> {
//                    if (n instanceof GraphNode<?> g && g.getValue() instanceof UUID id) {
//                        Post p = database.getPost(id);
//                        SimplePost simplePost = p.toSimplePost();
//
//                        if (!p.getAuthor().equals(key))
//                            simplePost.setRewin(key);
//
//                        return simplePost;
//                    }
//                    return null;
//                }).filter(Objects::nonNull).forEach(posts::add));

        for (Map.Entry<String, Set<Node>> entry : map.entrySet()) {
            String friends = entry.getKey();
            LinkedList<SimplePost> posts = new LinkedList<>();

            for (Node n : entry.getValue()) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof UUID id) {
                    Post p = database.getPost(id);
                    SimplePost simplePost = p.toSimplePost();

                    if (!p.getAuthor().equals(friends))
                        simplePost.setRewin(friends);

                    posts.add(simplePost);
                }
            }

            unpackedMap.put(friends, posts);
//            map.remove(friends);
        }

        try {
            out.sendObject(unpackedMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
