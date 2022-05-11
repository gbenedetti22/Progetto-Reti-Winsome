package com.unipi.database.requestHandler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.unipi.common.Console;
import com.unipi.common.SimpleComment;
import com.unipi.database.Database;
import com.unipi.database.DatabaseMain;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;
import com.unipi.database.utility.EntriesStorage;
import com.unipi.utility.ThreadWorker;
import com.unipi.utility.channelsio.ConcurrentChannelLineReceiver;
import com.unipi.utility.channelsio.PipedSelector;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/*
    Classe che si occupa della lettura e del parsing dei comandi ricevuti dal Server.
    Una volta processata la risposta, viene inoltrato un Packet a RequestWriter il quale lo inviarà al Client (vedi classe Packet)
 */
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
        ConcurrentChannelLineReceiver in = worker.getReceiver();

        in.setChannel(socket);

        try {
            String message = in.receiveLine();
            if (message == null) {
                socket.close();
                return;
            }

            if (print)
                System.out.println(message);

            if (!message.isEmpty())
                processRequest(message);

            selector.enqueue(socket, SelectionKey.OP_WRITE, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processRequest(String request) {
        String[] record = request.split(":", 2);
        record = Arrays.stream(record).map(String::trim).toArray(String[]::new);
        Console.log(request);
        switch (record[0]) {
            case "CREATE USER" -> createUser(record[1]);    // Registrazione
            case "FIND USER" -> findUser(record[1]);        // Login
            case "GET FRIENDS BY TAG" -> discoverFriendsByTag(record[1]);   // Trovare utenti che hanno un tag in comune
            case "GET FOLLOWERS OF" -> getFollowersOf(record[1]); // Reperire i followers
            case "GET FOLLOWING OF" -> getFollowingOf(record[1]);   // Reperire i following
            case "FOLLOW" -> followUser(record[1]);
            case "UNFOLLOW" -> unfollowUser(record[1]);
            case "CREATE POST" -> publishPost(record[1]);
            case "GET POST" -> getPost(record[1]); // Vedere un Post
            case "OPEN REWIN" ->
                    getRewin(record[1]); // Se il post che voglio vedere è un Rewin, allora viene invocata questa per un maggior controllo
            case "GET ALL POSTS OF" -> getAllPostsOf(record[1]); // Reperire tutti i Post di un utente
            case "GET FRIENDS POSTS OF" ->
                    getAllFriendsPosts(record[1]); // Reperire tutti i Post di tutti i follow di un utente (in pratica i Post che stanno nella Home)
            case "GET FRIENDS POST FROM DATE" ->
                    getLatestFriendsPostsOf(record[1]); // Reperire tutti i Post di tutti i follow di un utente usando la dateMap
            case "GET COMMENTS FROM DATE" ->
                    getLatestComments(record[1]); // Reperire tutti i commenti fatti dopo una certa data
            case "REWIN" -> rewin(record[1]);
            case "REMOVE REWIN" -> removeRewin(record[1]);
            case "COMMENT" -> addComment(record[1]);
            case "LIKE" -> addLike(record[1]);
            case "DISLIKE" -> addDislike(record[1]);
            case "REMOVE POST" -> removePost(record[1]);
            case "PULL NEW ENTRIES" -> getLatestEntries(); // Reperire le entries nuove per il calcolo delle ricompense
            case "UPDATE" -> updateUser(record[1]); // Assegnare le ricompense
            case "GET TRANSACTIONS" -> getTransactions(record[1]); // Ottenere la lista delle transazioni
            case "STOP", "QUIT", "EXIT", "CLOSE" -> DatabaseMain.safeClose();
            default -> {
            }
        }
    }

    //CREATE USER: USERNAME PASSWORD [TAGS1, TAGS2, ...]
    public void createUser(String record) {
        String[] data = record.trim().split(" ", 3);
        String username = data[0].trim();
        String password = data[1].trim();
        String[] tags = data[2].replace("[", "").replace("]", "").split(",");

        String code = database.createUser(username, password, List.of(tags));

        response = new Packet(Packet.FUNCTION.CREATE_USER, code);
    }

    //FIND USER: USERNAME PASSWORD
    public void findUser(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0];
        String password = data[1];

        Set<Node> set = database.getTagsIf(username, password);

        response = new Packet(Packet.FUNCTION.CHECK_IF_EXIST, set != null ? set : "206");
    }

    //GET FRIENDS BY TAG: USERNAME
    public void discoverFriendsByTag(String username) {
        Set<Node> set = database.getUsersByTag(username);

        response = new Packet(Packet.FUNCTION.DISCOVER, set != null ? set : "207");
    }

    //GET FOLLOWERS OF: USERNAME
    public void getFollowersOf(String username) {
        Set<String> set = database.getFollowersOf(username.trim());

        response = new Packet(Packet.FUNCTION.GET_FOLLOWERS, set != null ? set : "207");
    }

    //GET FOLLOWING OF: USERNAME
    public void getFollowingOf(String username) {
        Set<String> set = database.getFollowingOf(username);

        response = new Packet(Packet.FUNCTION.GET_FOLLOWING, set != null ? set : "207");
    }

    //FOLLOW: USERNAME USER
    public void followUser(String record) {
        String[] data = record.split(" ", 2);
        String u1 = data[0];
        String u2 = data[1];

        String code = database.followUser(u1, u2);

        response = new Packet(Packet.FUNCTION.FOLLOW, code);
    }

    //UNFOLLOW: USERNAME USER
    public void unfollowUser(String record) {
        String[] data = record.trim().split(" ", 2);
        String u1 = data[0].trim();
        String u2 = data[1].trim();

        String code = database.unfollowUser(u1, u2);

        response = new Packet(Packet.FUNCTION.UNFOLLOW, code);
    }

    //CREATE POST: AUTHOR TITLE CONTENT
    public void publishPost(String record) {
        String[] data = record.trim().split(" ", 3);
        String author = data[0].trim();
        String title = data[1].trim();
        String content = data[2].trim();

        title = new String(Base64.getDecoder().decode(title), StandardCharsets.UTF_8);
        content = new String(Base64.getDecoder().decode(content), StandardCharsets.UTF_8);
        Console.log("[DECODED]", title, content);

        Post p = database.createPost(author, title, content);

        response = new Packet(Packet.FUNCTION.CREATE_POST, p != null ? p : "207");
    }

    //GET POST: USERNAME IDPOST
    public void getPost(String record) {
        String[] data = record.trim().split(" ", 2);
        String whoWantToView = data[0].trim();
        String idPost = data[1].trim();

        HashMap<String, Object> map = database.viewFriendPost(whoWantToView, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.VIEW_POST, map != null ? (map.isEmpty() ? "215" : map) : "208");
    }

    //OPEN REWIN: AUTHOR IDPOST
    public void getRewin(String record) {
        String[] data = record.trim().split(" ", 2);
        String author = data[0].trim();
        String idPost = data[1].trim();

        HashMap<String, Object> map = database.openRewin(author, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.OPEN_REWIN, map != null ? (map.isEmpty() ? "216" : map) : "208");
    }

    //GET ALL POSTS OF: USERNAME
    public void getAllPostsOf(String username) {
        Set<Node> set = database.getAllPostsOf(username);

        response = new Packet(Packet.FUNCTION.GET_ALL_POSTS, set != null ? set : "207");
    }

    //GET COMMENTS FROM DATE: IDPOST DATE
    public void getLatestComments(String record) {
        String[] data = record.split(" ", 2);
        UUID idPost = UUID.fromString(data[0]);
        String date = data[1];

        try {
            Set<SimpleComment> set = database.getCommentsFromDate(idPost, date);
            response = new Packet(Packet.FUNCTION.GET_LATEST_COMMENTS, set != null ? set : "207");
        } catch (ParseException e) {
            response = new Packet(Packet.FUNCTION.GET_LATEST_COMMENTS, "204");
        }
    }

    //GET FRIENDS POSTS OF: USERNAME
    public void getAllFriendsPosts(String username) {
        Map<String, Set<Node>> map = database.getFriendsPostsOf(username);

        response = new Packet(Packet.FUNCTION.FRIENDS_POSTS, map != null ? map : "207");
    }

    //GET FRIENDS POST FROM DATE: USERNAME DATEMAP
    public void getLatestFriendsPostsOf(String record) {
        String[] data = record.split(" ", 2);
        String username = data[0];
        String json = data[1];

        Type mapType = new TypeToken<HashMap<String, String>>() {
        }.getType();
        HashMap<String, String> dateMap = new Gson().fromJson(json, mapType);
        Map<String, Set<Node>> posts;
        try {
            posts = database.getLatestFriendsPostsOf(username, dateMap);
        } catch (JsonSyntaxException e) {
            response = new Packet(Packet.FUNCTION.GET_LATEST_POST, "204");
            return;
        }

        response = new Packet(Packet.FUNCTION.GET_LATEST_POST, posts != null ? posts : "207");
    }

    //REWIN: USERNAME IDPOST
    public void rewin(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        String code = database.rewinFriendsPost(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.REWIN, code);
    }

    //REMOVE REWIN: USERNAME IDPOST
    public void removeRewin(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        String code = database.removeRewin(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.REMOVE_REWIN, code);
    }

    //COMMENT: IDPOST AUTHOR CONTENT
    public void addComment(String record) {
        String[] data = record.trim().split(" ", 3);
        String idPost = data[0].trim();
        String author = data[1].trim();
        String content = data[2].trim();

        Comment c;
        try {
            c = database.appendComment(UUID.fromString(idPost), author, content);
        } catch (UnsupportedOperationException e) {
            response = new Packet(Packet.FUNCTION.COMMENT, "217"); //Post not found
            return;
        }

        response = new Packet(Packet.FUNCTION.COMMENT, c != null ? c : "210"); //210 = comment not created
    }

    //LIKE: USERNAME IDPOST
    public void addLike(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        String code = database.appendLike(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.LIKE, code);
    }

    //DISLIKE: USERNAME IDPOST
    public void addDislike(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        String code = database.appendDislike(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.DISLIKE, code);
    }

    //REMOVE POST: USERNAME IDPOST
    public void removePost(String record) {
        String[] data = record.trim().split(" ", 2);
        String username = data[0].trim();
        String idPost = data[1].trim();

        String code = database.removePost(username, UUID.fromString(idPost));

        response = new Packet(Packet.FUNCTION.REMOVE_POST, code);
    }

    //PULL NEW ENTRIES
    public void getLatestEntries() {
        ArrayList<EntriesStorage.Entry> list = database.pullNewEntries();

        response = new Packet(Packet.FUNCTION.PULL_ENTRIES, list);
    }

    //UPDATE: user coins date idPost
    //UPDATE: [a, b, c ...] coins date

    // metodo per l assegnamento delle ricompense
    // i comandi possibili sono 2:
    // 1) update: user coins date idPost -> assegno la ricompensa al creatore del post
    // 2) update: [a, b, c ...] coins date -> assegno la ricompensa a tutti i curatori

    // nella 1) aggiorno anche il numero di interazioni su quel post
    private void updateUser(String s) {
        if (s.startsWith("[")) {
//            [a, b, c ...] coins date
            String array = s.substring(0, s.indexOf(']') + 1);  // [a, b, c ...]
            String[] data = s.substring(s.indexOf(']') + 1).trim().split(" ", 2); //[coins, date]

            String coins = data[0];
            String date = data[1];
            try {
                Database.getDateFormat().getSimpleDateFormat().parse(date);
            } catch (ParseException e) {
                e.printStackTrace();
                response = new Packet(Packet.FUNCTION.UPDATE_USER, "204"); // date format error
                return;
            }

            //converto la stringa "[a, b, c ...]" in un array di stringhe effettivo
            String[] users = array.replace("[", "").replace("]", "").split(",");
            StringBuilder sb = new StringBuilder();
            //per ogni utente, aggiungo la transazione
            for (String user : users) {
                User u = database.getUser(user.trim());
                if (u != null) {
                    u.addTransaction(coins, date);
                    continue;
                }

                sb.append(user).append(" ");
            }

            if (!sb.isEmpty()) {
                response = new Packet(Packet.FUNCTION.UPDATE_USER, sb.toString().trim());
                return;
            }

            response = new Packet(Packet.FUNCTION.UPDATE_USER, "200"); //tutto ok
            return;
        }

        String[] data = s.split(" ", 3);
        String user = data[0];
        String coins = data[1];
        String dateAndIdPost = data[2];
        int index = dateAndIdPost.lastIndexOf(' ');
        String date = dateAndIdPost.substring(0, index);
        UUID idPost = UUID.fromString(dateAndIdPost.substring(index + 1));

        Post p = database.getPost(idPost);
        p.incrementInteractions();

        try {
            Database.getDateFormat().getSimpleDateFormat().parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            response = new Packet(Packet.FUNCTION.UPDATE_USER, "204");  //date format error
            return;
        }

        User u = database.getUser(user);
        if (u != null) {
            u.addTransaction(coins, date);
            response = new Packet(Packet.FUNCTION.UPDATE_USER, "200"); //success
            return;
        }

        response = new Packet(Packet.FUNCTION.UPDATE_USER, "207");
    }

    //GET TRANSACTIONS: user
    private void getTransactions(String s) {
        User u = database.getUser(s);
        if (u != null) {
            response = new Packet(Packet.FUNCTION.GET_TRANSACTIONS, u.getTransactions());
            return;
        }

        response = new Packet(Packet.FUNCTION.GET_TRANSACTIONS, "207"); //user not found
    }
}
