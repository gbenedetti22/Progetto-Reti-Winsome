package com.unipi.database.requestHandler;

import com.unipi.common.SimpleComment;
import com.unipi.common.SimpleLike;
import com.unipi.common.SimplePost;
import com.unipi.database.DBResponse;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.database.utility.EntriesStorage;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.utility.ThreadWorker;
import com.unipi.utility.channelsio.ChannelLineSender;
import com.unipi.utility.channelsio.PipedSelector;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

/*
    Classe per inviare le risposte al Server.
    Questa classe riceve il pacchetto dalla classe RequestReader e lo inoltra al Server (vedi classe Packet)
 */
public class RequestWriter implements Runnable {
    private SocketChannel socket;
    private PipedSelector selector;
    private Packet packet;
    private ChannelLineSender out;
    private Database database;

    public RequestWriter(SocketChannel socket, PipedSelector selector, Packet p) {
        this.socket = socket;
        this.selector = selector;
        this.packet = p;
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
            case OPEN_REWIN -> getRewin();
            case GET_ALL_POSTS -> getAllPosts();
            case GET_LATEST_COMMENTS -> getLatestComments();
            case FRIENDS_POSTS -> friendsPosts();
            case REWIN -> rewin();
            case REMOVE_REWIN -> removeRewin();
            case COMMENT -> comment();
            case LIKE -> like();
            case DISLIKE -> dislike();
            case REMOVE_POST -> removePost();
            case PULL_ENTRIES -> getLatestEntries();
            case UPDATE_USER -> updateUser();
            case GET_TRANSACTIONS -> getTransactions();
            case CHECK_IF_EXIST -> findUser();
            case GET_LATEST_POST -> getLatestPost();
            default -> {
            }
        }

    }

    private void getLatestComments() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (packet.getMessage() instanceof Set<?> set) {
            try {
                out.sendObject(new DBResponse("200", set));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getRewin() {
        viewPost();
    }

    private void createUser() {
        String message = (String) packet.getMessage();
        try {
            out.sendObject(new DBResponse(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void findUser() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        Set<Node> set = (Set<Node>) packet.getMessage();
        List<String> l = new ArrayList<>();
        for (Node n : set) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof String tag) {
                l.add(tag);
            }
        }

        try {
            out.sendObject(new DBResponse("200", l));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void discover() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            if (packet.getMessage() instanceof Set<?> set) {
                ArrayList<String> users = new ArrayList<>(set.size());
                for (Object o : set) {
                    if (o instanceof Node n && n instanceof GroupNode g1) {
                        if (g1.getParent() instanceof GraphNode<?> g2 && g2.getValue() instanceof String s) {
                            users.add(s);
                        }
                    }
                }
                out.sendObject(new DBResponse("200", users));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getFollowers() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (packet.getMessage() instanceof Set<?> set) {
            try {
                out.sendObject(new DBResponse("200", set));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getFollowing() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (packet.getMessage() instanceof Set<?> set) {
            try {
                out.sendObject(new DBResponse("200", set));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void follow() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void unfollow() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPost() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        if (packet.getMessage() instanceof Post p) {
            try {
                out.sendObject(new DBResponse("200", p.toSimplePost()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void viewPost() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        try {
            if (!(packet.getMessage() instanceof HashMap<?, ?> message)) return;

            HashMap<String, Object> map = (HashMap<String, Object>) message;
            Set<Node> commentNodes = (Set<Node>) map.get("COMMENTS");
            Set<Node> likeNodes = (Set<Node>) map.get("LIKES");

            ArrayList<SimpleComment> comments = new ArrayList<>(commentNodes.size());
            ArrayList<SimpleLike> likes = new ArrayList<>(likeNodes.size());

            for (Node n : commentNodes) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                    comments.add(c.toSimpleComment());
                }
            }

            for (Node n : likeNodes) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                    likes.add(l.toSimpleLike());
                }
            }

            map.put("COMMENTS", comments);
            map.put("LIKES", likes);

            out.sendObject(new DBResponse("200", map));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getAllPosts() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

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

            out.sendObject(new DBResponse("200", posts));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void friendsPosts() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        if (!(packet.getMessage() instanceof Map<?, ?> m)) return;

        Map<String, Set<Node>> map = (Map<String, Set<Node>>) m;
        HashMap<String, LinkedList<SimplePost>> unpackedMap = new HashMap<>(map.size());
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
        }

        try {
            out.sendObject(new DBResponse("200", unpackedMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void getLatestPost() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        if (!(packet.getMessage() instanceof Map<?, ?> m)) return;

        Map<String, Set<Node>> map = (Map<String, Set<Node>>) m;
        HashMap<String, ArrayList<SimplePost>> unpackedMap = new HashMap<>(map.size());

        for (Map.Entry<String, Set<Node>> entry : map.entrySet()) {
            String friends = entry.getKey();
            ArrayList<SimplePost> posts = new ArrayList<>();

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
        }

        try {
            out.sendObject(new DBResponse("200", unpackedMap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rewin() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeRewin() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void comment() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if(packet.getMessage() instanceof Comment c) {
            try {
                out.sendObject(new DBResponse("200", c.toSimpleComment()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void like() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dislike() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removePost() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getTransactions() {
        if (packet.getMessage() instanceof String code) {
            try {
                out.sendObject(new DBResponse(code));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (packet.getMessage() instanceof List<?> transactions) {
            try {
                out.sendObject(new DBResponse("200", transactions));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void getLatestEntries() {
        try {
            if (packet.getMessage() instanceof ArrayList<?> entries) {
                out.sendInteger(entries.size());

                for (Object obj : entries) {
                    if (obj instanceof EntriesStorage.Entry e) {
                        out.sendObject(e);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUser() {
        if (packet.getMessage() instanceof String message) {
            try {
                if(message.equals("200"))
                    out.sendObject(new DBResponse("200"));
                else
                    out.sendObject(new DBResponse("214", message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
