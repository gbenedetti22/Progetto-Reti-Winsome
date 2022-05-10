package com.unipi.server.requestHandler;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.unipi.common.*;
import com.unipi.database.DBResponse;
import com.unipi.database.tables.Like;
import com.unipi.server.ParamsValidator;
import com.unipi.server.RMI.FollowersDatabase;
import com.unipi.server.ServerMain;
import com.unipi.server.ServerProperties;
import com.unipi.server.ServerThreadWorker;
import com.unipi.utility.channelsio.ChannelLineSender;
import com.unipi.utility.channelsio.ConcurrentChannelLineReceiver;
import com.unipi.utility.channelsio.PipedSelector;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

/*
    Classe che gestisce le richieste del client.
    Questa classe implementa Callable perchè viene utilizzata anche per l RMI (vedi metodo di registrazione) e quindi
    è necessaria una risposta
 */
public class ServerRequestReader implements Callable<String> {
    private SocketChannel socket; // Socket del client
    private PipedSelector selector; // Selector per la comunicazione con il client
    private Gson gson; // Gson per la serializzazione/deserializzazione dei messaggi
    private ConcurrentChannelLineReceiver in;
    private ChannelLineSender out;
    private SocketChannel db_connection; // Socket per la comunicazione con il database

    // Se impostato, il thread corrente non si metterà in ascolto sul Socket ma soddisferà la richiesta
    // Serve per l RMI, in quanto il Client remoto imposterà una richiesta "manualmente"
    private WSRequest request;
    private WSResponse response;
    private Map<SocketChannel, String> logTable; // tabella degli utenti loggati
    private String username; // username del client corrente

    public ServerRequestReader(SocketChannel socket, PipedSelector selector) {
        this.socket = socket;
        this.selector = selector;
        this.request = null;
        this.response = new WSResponse();
        this.logTable = ServerMain.getUsersLoggedTable();
        this.username = logTable.get(socket);

        gson = new GsonBuilder().setDateFormat("dd/MM/yy - HH:mm:ss").create();
    }

    public ServerRequestReader(WSRequest request) {
        this.request = request;
        this.response = null;
        this.logTable = ServerMain.getUsersLoggedTable();
    }

    @Override
    public String call() {
        ServerThreadWorker worker = (ServerThreadWorker) Thread.currentThread();
        in = worker.getReceiver();
        out = worker.getSender();
        db_connection = worker.getDatabaseSocket();
        String s = null;

        if (request != null) {
            return processRequest(request);
        }

        in.setChannel(socket);

        try {
            String message = in.receiveLine();
            if (message == null) {  // Se ho raggiunto EOF, il Client è uscito (anche brutalmente), quindi chiudo la connessione
                ServerMain.unregisterClient(username);
                ServerMain.getUsersLoggedTable().remove(socket);

                socket.close();
                return "CLOSED SOCKET";
            }

            Console.log(message);
            try {
                WSRequest request = gson.fromJson(message, WSRequest.class);
                if(request.getOp() == null) return "NULL REQUEST";

                s = processRequest(request);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            } finally {
                selector.enqueue(socket, SelectionKey.OP_WRITE, response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return s;
    }

    private String processRequest(WSRequest request) {
        switch (request.getOp()) {
            case CREATE_USER -> {
                return performRegistration(request);
            }

            case LOGIN -> {
                return performLogin(request);
            }

            case GET_FRIENDS_BY_TAG -> {
                return performListUsers();
            }

            case GET_FOLLOWERS -> {
                return performGetFollowers(request);
            }

            case GET_FOLLOWING -> {
                return performGetFollowing();
            }

            case FOLLOW -> {
                return performFollow(request);
            }

            case UNFOLLOW -> {
                return performUnfollow(request);
            }

            case CREATE_POST -> {
                return performPublishPost(request);
            }

            case GET_POST -> {
                return performGetPost(request);
            }

            case OPEN_REWIN -> {
                return performOpenRewin(request);
            }

            case GET_MY_POSTS -> {
                return performGetAllPosts();
            }

            case GET_FRIENDS_POSTS -> {
                return performGetFeed();
            }

            case GET_FRIENDS_POST_FROM_DATE -> {
                return performGetLatestPost(request);
            }

            case GET_COMMENTS_FROM_DATE -> {
                return performGetLatestComments(request);
            }

            case REWIN -> {
                return performRewin(request);
            }

            case REMOVE_REWIN -> {
                return performRemoveRewin(request);
            }

            case COMMENT -> {
                return performAddComment(request);
            }

            case LIKE -> {
                return performAddLike(request);
            }

            case DISLIKE -> {
                return performAddDislike(request);
            }

            case REMOVE_POST -> {
                return performRemovePost(request);
            }

            case PULL_NEW_ENTRIES -> {
                //Vedi classe: RewardCalculator
            }

            case GET_TRANSACTIONS -> {
                return performGetTransactions();
            }

            case LOGOUT -> {
                return performLogout();
            }
        }

        String s = "Richiesta non valida";
        response = WSResponse.newErrorResponse(s);
        return s;
    }

    @SuppressWarnings("unchecked")
    private String performGetLatestComments(WSRequest request) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkGetLatestComments(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String idPost = (String) request.getParams()[0];
        String date = (String) request.getParams()[1];

        String command = String.format("GET COMMENTS FROM DATE: %s %s", idPost, date);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            DBResponse db_response = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(db_response.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            Set<SimpleComment> set = (Set<SimpleComment>) db_response.getMessage();
            TreeSet<SimpleComment> comments = new TreeSet<>(set);

            response = WSResponse.newSuccessResponse(gson.toJson(comments));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    // Metodo per eseguire la registrazione.
    // Viene inviato il comando create user per creare un utente sul DB
    // Questo metodo viene usato mediante RMI, quindi:
    // 1) il client chiama un metodo stub che inizializza questa classe e la passa ad un thread pool
    // 2) Quando un thread è libero, viene invocato questo metodo (WSRequest sarà diverso da null)
    // 3) Essendo un interfaccia Callable, il risultato dell operazione sarà ritornato sotto forma di string
    private String performRegistration(WSRequest request) {
        Object[] params = request.getParams();

        String s = ParamsValidator.checkRegisterParams(params);
        if (!s.equals("OK")) {
            return s;
        }

        String username = String.valueOf(params[0]);
        String password = String.valueOf(params[1]);
        String tags = params[2].toString();

        String command = String.format("CREATE USER: %s %s %s", username, password, tags);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            DBResponse db_response = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(db_response.getCode());
            if (!conv.equals("OK")) {
                return conv;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performLogin(WSRequest request) {
        Object[] params = request.getParams();
        String s = ParamsValidator.checkLoginParams(params);
        if (!s.equals("OK")) {
            response.setStatus(WSResponse.CODES.ERROR);
            response.setBody(s);
            return s;
        }

        // Da qui in poi, i parametri ricevuti dal client sono corretti
        String username = (String) params[0];
        String password = (String) params[1];
        if(username.isBlank() && password.isBlank()) {
            String s1 = "Username e password non possono essere vuoti";
            response = WSResponse.newErrorResponse(s1);
            return s1;
        }

        if(ServerMain.getUsersLoggedTable().containsValue(username)){
            String s1 = "Questo utente ha già eseguito il login su un altro dispositivo";
            response = WSResponse.newErrorResponse(s1);
            return s1;
        }

        out.setChannel(db_connection);
        in.setChannel(db_connection);

        String command = String.format("FIND USER: %s %s", username, password);
        try {
            out.sendLine(command);

            DBResponse response = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(response.getCode());
            if (!conv.equals("OK")) {
                this.response.setStatus(WSResponse.CODES.ERROR);
                this.response.setBody(conv);
                Console.log(response.getCode(), conv);
                return conv;
            }

            Console.log(response);

            ArrayList<String> tags = (ArrayList<String>) response.getMessage();
            if (tags.isEmpty()) {
                String s1 = "Utente non trovato";

                this.response.setStatus(WSResponse.CODES.ERROR);
                this.response.setBody(s1);
                return s1;
            }

            tags.sort(Comparator.naturalOrder());
            String multicastAddress = (String) ServerProperties.getValue(ServerProperties.NAMES.MULTICAST_ADDRESS);
            int multicastPort = (int) ServerProperties.getValue(ServerProperties.NAMES.MULTICAST_PORT);
            LoginResponse logResponse = new LoginResponse(tags, multicastAddress, multicastPort);

            this.response = WSResponse.newSuccessResponse(gson.toJson(logResponse));
            logTable.put(socket, username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performListUsers() {
        if (username != null) {
            String command = String.format("GET FRIENDS BY TAG: %s", username);
            out.setChannel(db_connection);
            in.setChannel(db_connection);

            try {
                out.sendLine(command);
                DBResponse dbResponse = (DBResponse) in.receiveObject();
                String conv = CodeConverter.convert(dbResponse.getCode());

                if (!conv.equals("OK")) {
                    response.setStatus(WSResponse.CODES.ERROR);
                    response.setBody(conv);
                    return conv;
                }

                ArrayList<String> users = (ArrayList<String>) dbResponse.getMessage();
                users.remove(username);

                Collections.sort(users);

                String json = gson.toJson(users);

                response = WSResponse.newSuccessResponse(json);
                return "OK";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String s = "Utente non loggato";
        response.setStatus(WSResponse.CODES.ERROR);
        response.setBody(s);
        return s;
    }

    @SuppressWarnings("unchecked")
    private String performGetFollowing() {
        if (username != null) {
            String command = String.format("GET FOLLOWING OF: %s", username);
            out.setChannel(db_connection);
            in.setChannel(db_connection);

            try {
                out.sendLine(command);
                DBResponse dbResponse = (DBResponse) in.receiveObject();
                String conv = CodeConverter.convert(dbResponse.getCode());
                if (!conv.equals("OK")) {
                    response.setStatus(WSResponse.CODES.ERROR);
                    response.setBody(conv);
                    return conv;
                }

                Set<String> tree = (Set<String>) dbResponse.getMessage();

                String json = gson.toJson(tree);

                response.setStatus(WSResponse.CODES.OK);
                response.setBody(json);
                return "OK";
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String s = "Utente non loggato";
        response.setStatus(WSResponse.CODES.ERROR);
        response.setBody(s);
        return s;
    }

    @SuppressWarnings("unchecked")
    private String performGetFollowers(WSRequest request) {
        String username = (String) request.getParams()[0];

        String command = String.format("GET FOLLOWERS OF: %s", username);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response.setStatus(WSResponse.CODES.ERROR);
                response.setBody(conv);
                return conv;
            }

            Set<String> set = (Set<String>) dbResponse.getMessage();
            TreeSet<String> sortedSet = new TreeSet<>(set);

            // I followers devono essere aggiornati tramite RMI
            // L interfaccia su cui questi vengono settati, viene presa dal main
            FollowersDatabase clientFollowers = ServerMain.getCallback(username);
            if (clientFollowers == null) {
                String s = "Utente: " + username + " non registrato al servizio di followers";
                Console.err(s);
                return s;
            }

            clientFollowers.setFollowers(sortedSet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    private String performFollow(WSRequest request) {
        String check = ParamsValidator.checkFollowParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        if (username != null) {
            String userToFollow = (String) request.getParams()[0];
            String command = String.format("FOLLOW: %s %s", username, userToFollow);
            out.setChannel(db_connection);
            in.setChannel(db_connection);

            try {
                out.sendLine(command);
                DBResponse dbResponse = (DBResponse) in.receiveObject();
                String conv = CodeConverter.convert(dbResponse.getCode());
                if (!conv.equals("OK")) {
                    response = WSResponse.newErrorResponse(conv);
                    return conv;
                }

                //Supponiamo che l utente A voglia seguire l utente B:
                // A -> username
                // B -> userToFollow
                // A segue B quindi va nella lista dei followers di B
                FollowersDatabase clientFollowers = ServerMain.getCallback(userToFollow);
                if (clientFollowers != null)
                    clientFollowers.addFollower(username);

                response = WSResponse.newSuccessResponse();
                return "OK";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String s = "Utente non loggato";
        response = WSResponse.newErrorResponse(s);
        return s;
    }

    private String performUnfollow(WSRequest request) {
        String check = ParamsValidator.checkFollowParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        if (username != null) {
            String userToFollow = (String) request.getParams()[0];
            String command = String.format("UNFOLLOW: %s %s", username, userToFollow);
            out.setChannel(db_connection);
            in.setChannel(db_connection);

            try {
                Console.log(command);
                out.sendLine(command);
                DBResponse dbResponse = (DBResponse) in.receiveObject();
                String conv = CodeConverter.convert(dbResponse.getCode());
                if (!conv.equals("OK")) {
                    response = WSResponse.newErrorResponse(conv);
                    return conv;
                }

                FollowersDatabase clientFollowers = ServerMain.getCallback(userToFollow);
                if (clientFollowers != null)
                    clientFollowers.removeFollower(username);

                response = WSResponse.newSuccessResponse();
                return "OK";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String s = "Utente non loggato";
        response = WSResponse.newErrorResponse(s);
        return s;
    }

    private String performPublishPost(WSRequest request) {
        String author = ServerMain.getUsersLoggedTable().get(socket);
        if (author == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        Object[] params = request.getParams();
        String check = ParamsValidator.checkPublishPostParams(params);
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }
        String title = (String) params[0];
        String content = (String) params[1];

        title = Base64.getEncoder().encodeToString(title.getBytes(StandardCharsets.UTF_8));
        content = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

        String command = String.format("CREATE POST: %s %s %s", author, title, content);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            SimplePost post = (SimplePost) dbResponse.getMessage();
            response = WSResponse.newSuccessResponse(gson.toJson(post));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performGetAllPosts() {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String command = String.format("GET ALL POSTS OF: %s", username);
        out.setChannel(db_connection);
        in.setChannel(db_connection);
        TreeSet<SimplePost> tree = new TreeSet<>(Comparator.reverseOrder());

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            LinkedList<SimplePost> posts = (LinkedList<SimplePost>) dbResponse.getMessage();
            for (SimplePost p : posts) {
                if (!p.getAuthor().equals(username))
                    p.setRewin(username);

                tree.add(p);
            }

            String json = gson.toJson(tree);
            response = WSResponse.newSuccessResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performGetFeed() {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String command = String.format("GET FRIENDS POSTS OF: %s", username);
        out.setChannel(db_connection);
        in.setChannel(db_connection);
        TreeSet<SimplePost> tree = new TreeSet<>(Comparator.reverseOrder());

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            HashMap<String, LinkedList<SimplePost>> map = (HashMap<String, LinkedList<SimplePost>>) dbResponse.getMessage();
            for (List<SimplePost> l : map.values()) {
                tree.addAll(l);
            }

            String json = gson.toJson(tree);
            response = WSResponse.newSuccessResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performGetLatestPost(WSRequest request) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkGetLatestPost(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String dateMap = (String) request.getParams()[0];

        String command = String.format("GET FRIENDS POST FROM DATE: %s %s", username, dateMap);
        out.setChannel(db_connection);
        in.setChannel(db_connection);
        TreeSet<SimplePost> tree = new TreeSet<>(Comparator.reverseOrder());

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            HashMap<String, LinkedList<SimplePost>> map = (HashMap<String, LinkedList<SimplePost>>) dbResponse.getMessage();
            for (List<SimplePost> l : map.values()) {
                tree.addAll(l);
            }

            Console.log("LATEST POST", map);

            String json = gson.toJson(tree);
            response = WSResponse.newSuccessResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    private String performRewin(WSRequest request) {
        String command = "REWIN: %s %s";

        return sendSimpleCommand(request, command);
    }

    private String performRemoveRewin(WSRequest request) {
        String command = "REMOVE REWIN: %s %s";

        return sendSimpleCommand(request, command);
    }

    private String performAddLike(WSRequest request) {
        String command = "LIKE: %s %s";

        return sendSimpleCommand(request, command);
    }

    private String performAddDislike(WSRequest request) {
        String command = "DISLIKE: %s %s";

        return sendSimpleCommand(request, command);
    }

    private String performRemovePost(WSRequest request) {
        String command = "REMOVE POST: %s %s";

        return sendSimpleCommand(request, command);
    }

    private String performAddComment(WSRequest request) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkCommentParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String id = (String) request.getParams()[0];
        String content = (String) request.getParams()[1];

        String command = String.format("COMMENT: %s %s %s", id, username, content);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            response = WSResponse.newSuccessResponse(gson.toJson(dbResponse.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performGetPost(WSRequest request) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkPostActionParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String id = (String) request.getParams()[0];

        String command = String.format("GET POST: %s %s", username, id);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            Map<String, Object> map = (Map<String, Object>) dbResponse.getMessage();

            ArrayList<SimpleComment> comments = (ArrayList<SimpleComment>) map.get("COMMENTS");
            ArrayList<SimpleLike> likes = (ArrayList<SimpleLike>) map.get("LIKES");

            comments.sort(Comparator.reverseOrder());

            long nLikes = 0;
            long nDislikes = 0;

            for (SimpleLike l : likes) {
                if (l.getType() == Like.TYPE.LIKE) {
                    nLikes++;
                } else {
                    nDislikes++;
                }
            }

            map.put("LIKES", nLikes);
            map.put("DISLIKES", nDislikes);

            Type responseType = new TypeToken<HashMap<String, String>>() {
            }.getType();
            String json = gson.toJson(map, responseType);
            response = WSResponse.newSuccessResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performOpenRewin(WSRequest request) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkOpenRewinParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String author = (String) request.getParams()[0];
        String id = (String) request.getParams()[1];

        String command = String.format("OPEN REWIN: %s %s", author, id);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            Map<String, Object> map = (Map<String, Object>) dbResponse.getMessage();

            ArrayList<SimpleComment> comments = (ArrayList<SimpleComment>) map.get("COMMENTS");
            ArrayList<SimpleLike> likes = (ArrayList<SimpleLike>) map.get("LIKES");

            comments.sort(Comparator.reverseOrder());

            long nLikes = 0;
            long nDislikes = 0;

            for (SimpleLike l : likes) {
                if (l.getType() == Like.TYPE.LIKE) {
                    nLikes++;
                } else {
                    nDislikes++;
                }
            }

            map.put("LIKES", nLikes);
            map.put("DISLIKES", nDislikes);

            Type responseType = new TypeToken<HashMap<String, String>>() {
            }.getType();
            String json = gson.toJson(map, responseType);

            response = WSResponse.newSuccessResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    @SuppressWarnings("unchecked")
    private String performGetTransactions() {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String command = "GET TRANSACTIONS: " + username;
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);
            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            List<WinsomeTransaction> transactions = (List<WinsomeTransaction>) dbResponse.getMessage();
            Collections.sort(transactions);

            response = WSResponse.newSuccessResponse(gson.toJson(transactions));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    private String sendSimpleCommand(WSRequest request, String c) {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        if (username == null) {
            String s = "Utente non loggato";
            response = WSResponse.newErrorResponse(s);
            return s;
        }

        String check = ParamsValidator.checkPostActionParams(request.getParams());
        if (!check.equals("OK")) {
            response = WSResponse.newErrorResponse(check);
            return check;
        }

        String id = (String) request.getParams()[0];

        String command = String.format(c, username, id);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            DBResponse dbResponse = (DBResponse) in.receiveObject();
            String conv = CodeConverter.convert(dbResponse.getCode());
            if (!conv.equals("OK") && !conv.equals("CHANGED LIKE")) {
                response = WSResponse.newErrorResponse(conv);
                return conv;
            }

            response = WSResponse.newSuccessResponse(conv);

            if (command.startsWith("REMOVE POST"))
                ServerMain.sendUDPMessage(String.format("REMOVED %s %s", username, id));

            if (command.startsWith("REMOVE REWIN"))
                ServerMain.sendUDPMessage(String.format("REMOVED REWIN %s %s", username, id));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }


    private String performLogout() {
        String username = ServerMain.getUsersLoggedTable().get(socket);
        ServerMain.getUsersLoggedTable().remove(socket);
        ServerMain.unregisterClient(username);  //è una sicurezza in più in quanto il client già si unregistra

        response = WSResponse.newSuccessResponse();
        return "OK";
    }
}
