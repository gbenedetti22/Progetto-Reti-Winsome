package com.unipi.database.requestHandler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Post;
import com.unipi.database.utility.ThreadWorker;
import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.utility.channelsio.ConcurrentChannelReceiver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;

public class RequestReader implements Runnable {
    private SocketChannel socket;
    private PipedSelector selector;
    private Database database;
    private Packet response;
    private boolean print = false;

    public RequestReader(SocketChannel socket, PipedSelector selector) {
        this.socket = socket;
        this.selector = selector;
        this.database = DatabaseMain.getDatabase();
        this.response = Packet.newEmptyPacket();
    }

    public RequestReader(SocketChannel socket, PipedSelector selector, boolean debug) {
        this(socket, selector);
        print = debug;
    }

    @Override
    public void run() {
        ThreadWorker worker = (ThreadWorker) Thread.currentThread();
        ConcurrentChannelReceiver in = worker.getReceiver();

        in.setChannel(socket);

        try {
            String message = in.receiveLine();
            if(message == null) {
                socket.close();
                return;
            }

            if (print)
                System.out.println(message);

            if(!message.isEmpty())
                processRequest(message);

            selector.enqueue(socket, SelectionKey.OP_WRITE, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequest(String request) {
        String[] record = request.split(":", 2);
        record = Arrays.stream(record).map(String::trim).toArray(String[]::new);
        switch (record[0]) {
            case "CREATE USER" -> createUser(record[1]);
            case "FIND USER" -> findUser(record[1]);
            case "GET FRIENDS BY TAG" -> discoverFriendsByTag(record[1]);
            case "GET FOLLOWERS OF" -> getFollowersOf(record[1]);
            case "GET FOLLOWING OF" -> getFollowingOf(record[1]);
            case "FOLLOW" -> followUser(record[1]);
            case "UNFOLLOW" -> unfollowUser(record[1]);
            case "CREATE POST" -> publishPost(record[1]);
            case "GET POST" -> getPost(record[1]);
            case "GET ALL POSTS OF" -> getAllPostsOf(record[1]);
            case "GET FRIENDS POSTS OF" -> getAllFriendsPosts(record[1]);
            case "GET FRIENDS POST FROM DATE" -> getLatestFriendsPostsOf(record[1]);
            case "REWIN" -> rewin(record[1]);
            case "REMOVE REWIN" -> removeRewin(record[1]);
            case "COMMENT" -> addComment(record[1]);
            case "LIKE" -> addLike(record[1]);
            case "DISLIKE" -> addDislike(record[1]);
            case "REMOVE POST" -> removePost(record[1]);
            case "PULL NEW ENTRIES" -> getLatestEntries();
            case "STOP", "QUIT", "EXIT", "CLOSE" -> DatabaseMain.stop();
            default -> {
            }
        }

        database.save();
    }

    //CREATE USER: USERNAME PASSWORD [TAGS1, TAGS2, ...]
    public void createUser(String record) {
        String[] data = record.trim().split(" ", 3);
        String username = data[0].trim();
        String password = data[1].trim();
        String[] tags = data[2].replace("[", "").replace("]", "").split(",");

        boolean inserted = database.createUser(username, password, List.of(tags));

        response = new Packet(Packet.FUNCTION.CREATE_USER, inserted ? "OK" : "USER ALREADY EXIST");
    }

    //FIND USER: USERNAME PASSWORD
    public void findUser(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0];
        String password = data[1];

        Set<Node> set = database.getTagsIf(username, password);

        response = new Packet(Packet.FUNCTION.CHECK_IF_EXIST, set);
    }

    //GET FRIENDS BY TAG: USERNAME
    public void discoverFriendsByTag(String username) {
        Set<Node> set = database.getUsersByTag(username);

        response = new Packet(Packet.FUNCTION.DISCOVER, set);
    }

    //GET FOLLOWERS OF: USERNAME
    public void getFollowersOf(String username) {
        Set<String> set = database.getFollowersOf(username.trim());

        response = new Packet(Packet.FUNCTION.GET_FOLLOWERS, set);
    }

    //GET FOLLOWING OF: USERNAME
    public void getFollowingOf(String username) {
        Set<String> set = database.getFollowingOf(username);

        response = new Packet(Packet.FUNCTION.GET_FOLLOWING, set);
    }

    //FOLLOW: USERNAME USER
    public void followUser(String record) {
        String[] data = record.split(" ", 2);
        String u1 = data[0];
        String u2 = data[1];

        boolean followed = database.followUser(u1, u2);

        response = new Packet(Packet.FUNCTION.FOLLOW, followed ? "OK" : "ALREADY FOLLLOWING");
    }

    //UNFOLLOW: USERNAME USER
    public void unfollowUser(String record) {
        String[] data = record.trim().split(" ", 2);
        String u1 = data[0].trim();
        String u2 = data[1].trim();

        boolean unfollowed = database.unfollowUser(u1, u2);

        response = new Packet(Packet.FUNCTION.UNFOLLOW, unfollowed ? "OK" : "NOT FOLLLOWING");
    }

    //CREATE POST: AUTHOR TITLE CONTENT
    public void publishPost(String record) {
        String[] data = record.trim().split(" ", 3);
        String author = data[0].trim();
        String title = data[1].trim();
        String content = data[2].trim();

        Post p = database.createPost(author, title, content);

        response = new Packet(Packet.FUNCTION.CREATE_POST, p);
    }

    //GET POST: USERNAME IDPOST
    public void getPost(String record) {
        String[] data = record.trim().split(" ", 2);
        String whoWantToView = data[0].trim();
        String idPost = data[1].trim();

        HashMap<String, Object> map = database.viewFriendPost(whoWantToView, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.VIEW_POST, map);
    }

    //GET ALL POSTS OF: USERNAME
    public void getAllPostsOf(String username) {
        Set<Node> set = database.getAllPostsOf(username);

        response = new Packet(Packet.FUNCTION.GET_ALL_POSTS, set);
    }

    //GET FRIENDS POSTS OF: USERNAME
    public void getAllFriendsPosts(String username) {
        Map<String, Set<Node>> map = database.getFriendsPostsOf(username);

        response = new Packet(Packet.FUNCTION.FRIENDS_POSTS, map);
    }

    //GET FRIENDS POST FROM DATE: USERNAME JSON
    private Type mapType = new TypeToken<HashMap<String, String>>(){}.getType();
    public void getLatestFriendsPostsOf(String record) {
        String[] data = record.split(" ", 2);
        String username = data[0];
        String json = data[1];

        HashMap<String, String> dateMap = new Gson().fromJson(json, mapType);
        Map<String, Set<Node>> posts;
        try {
            posts = database.getLatestFriendsPostsOf(username, dateMap);
        } catch (JsonSyntaxException e) {
            response = new Packet(Packet.FUNCTION.GET_LATEST_POST, "Map error: " + dateMap);
            return;
        }

        response = new Packet(Packet.FUNCTION.GET_LATEST_POST, posts);
    }

    //REWIN: USERNAME IDPOST
    public void rewin(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        boolean rewinned = database.rewinFriendsPost(username, UUID.fromString(idPost));

        //TODO: modifcare i casi di errore
        response = new Packet(Packet.FUNCTION.REWIN, rewinned ? "OK" : "NOT FOLLOWING");
    }

    //REMOVE REWIN: USERNAME IDPOST
    public void removeRewin(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        boolean rewinned = database.removeRewin(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.REMOVE_REWIN, rewinned ? "OK" : "NOT EXIST");
    }

    //COMMENT: USERNAME IDPOST AUTHOR CONTENT
    public void addComment(String record) {
        String[] data = record.trim().split(" ", 4);
        String username = data[0].trim();
        String idPost = data[1].trim();
        String author = data[2].trim();
        String content = data[3].trim();

        boolean added = database.appendComment(username, UUID.fromString(idPost), author, content);

        response = new Packet(Packet.FUNCTION.COMMENT, added ? "OK" : "POST NOT EXIST");
    }

    //LIKE: USERNAME IDPOST
    public void addLike(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        boolean added = database.appendLike(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.LIKE, added ? "OK" : "NOT EXIST");
    }

    //DISLIKE: USERNAME IDPOST
    public void addDislike(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        boolean added = database.appendDislike(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.DISLIKE, added ? "OK" : "NOT EXIST");
    }

    //REMOVE POST: USERNAME IDPOST
    public void removePost(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        boolean removed = database.removePost(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.REMOVE_POST, removed ? "OK" : "NOT EXIST");
    }

    //PULL NEW ENTRIES
    public void getLatestEntries() {
        Set<Node> set = database.pullNewEntries();

        response = new Packet(Packet.FUNCTION.PULL_ENTRIES, set);
    }
}
