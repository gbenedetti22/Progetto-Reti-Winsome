package com.unipi.server.requestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.unipi.common.SimpleComment;
import com.unipi.common.SimpleLike;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.server.ParamsValidator;
import com.unipi.server.RMI.FollowersDatabase;
import com.unipi.server.ServerMain;
import com.unipi.server.ServerThreadWorker;
import com.unipi.utility.channelsio.ChannelSender;
import com.unipi.utility.channelsio.PipedSelector;
import com.unipi.utility.channelsio.ConcurrentChannelReceiver;
import com.unipi.common.Console;
import com.unipi.common.SimplePost;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Callable;

public class ServerRequestReader implements Callable<String> {
    private SocketChannel socket;
    private PipedSelector selector;
    private Gson gson;
    private ConcurrentChannelReceiver in;
    private ChannelSender out;
    private SocketChannel db_connection;
    private WSRequest request;
    private WSResponse response;
    private Map<SocketChannel, String> logTable;
    private String username;

    public ServerRequestReader(SocketChannel socket, PipedSelector selector) {
        this.socket = socket;
        this.selector = selector;
        this.request = null;
        this.response = new WSResponse();
        this.logTable = ServerMain.getUsersLoggedTable();
        this.username = logTable.get(socket);

        gson = new GsonBuilder().setDateFormat("dd/MM/yy - hh:mm:ss").create();
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
            if (message == null) {
                socket.close();
                ServerMain.unregisterClient(username);
                return "CLOSED SOCKET";
            }

            Console.log(message);
            try {
                WSRequest request = gson.fromJson(message, WSRequest.class);
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

            case FIND_USER -> {
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

            case GET_MY_POSTS -> {
                return performGetAllPosts();
            }

            case GET_FRIENDS_POSTS -> {
                return performGetFeed();
            }

            case GET_FRIENDS_POST_FROM_DATE -> {
                return performGetLatestPost(request);
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
        }

        String s = "Richiesta non valida";
        response = WSResponse.newErrorResponse(s);
        return s;
    }

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

            String db_response = in.receiveLine();
            if (!db_response.equals("OK")) {
                return s;
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

        String username = (String) params[0];
        String password = (String) params[1];
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        String command = String.format("FIND USER: %s %s", username, password);
        try {
            out.sendLine(command);

            //in db_response devono esserci i suoi tag
            List<String> db_response = (List<String>) in.receiveObject();
            if (db_response.isEmpty()) {
                String s1 = "Utente non trovato";

                response.setStatus(WSResponse.CODES.ERROR);
                response.setBody(s1);
                return s1;
            }

            db_response.sort(Comparator.naturalOrder());

            response.setStatus(WSResponse.CODES.OK);
            response.setBody(gson.toJson(db_response));
            logTable.put(socket, username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }

    private String performListUsers() {
        if (username != null) {
            String command = String.format("GET FRIENDS BY TAG: %s", username);
            out.setChannel(db_connection);
            in.setChannel(db_connection);

            try {
                out.sendLine(command);
                TreeSet<String> tree = new TreeSet<>();

                int size = in.receiveInteger();

                for (int i = 0; i < size; i++) {
                    String user = in.receiveLine();
                    if(user.equals(username)) continue;

                    tree.add(user);
                }

                String json = gson.toJson(tree);

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
                Set<String> tree = (Set<String>) in.receiveObject();

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

            Set<String> set = (Set<String>) in.receiveObject();
            TreeSet<String> sortedSet = new TreeSet<>(set);

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
                String dbResponse = in.receiveLine();

                if (!dbResponse.equals("OK")) {
                    response = WSResponse.newErrorResponse(dbResponse);
                    return dbResponse;
                }

                //Supponiamo che l utente A voglia seguire l utente B:
                // A -> username
                // B -> userToFollow
                // A segue B quindi va nella lista dei followers di B
                FollowersDatabase clientFollowers = ServerMain.getCallback(userToFollow);
                if(clientFollowers != null)
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
                out.sendLine(command);
                String dbResponse = in.receiveLine();

                if (!dbResponse.equals("OK")) {
                    response = WSResponse.newErrorResponse(dbResponse);
                    return dbResponse;
                }

                FollowersDatabase clientFollowers = ServerMain.getCallback(userToFollow);
                if(clientFollowers != null)
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

        String command = String.format("CREATE POST: %s %s %s", author, title, content);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);
            SimplePost post = (SimplePost) in.receiveObject();
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
            LinkedList<SimplePost> posts = (LinkedList<SimplePost>) in.receiveObject();
            for (SimplePost p : posts) {
                if(!p.getAuthor().equals(username))
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

            HashMap<String, LinkedList<SimplePost>> map = (HashMap<String, LinkedList<SimplePost>>) in.receiveObject();
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

            HashMap<String, LinkedList<SimplePost>> map = (HashMap<String, LinkedList<SimplePost>>) in.receiveObject();
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

        String command = String.format("COMMENT: %s %s %s %s", username, id, username, content);
        out.setChannel(db_connection);
        in.setChannel(db_connection);

        try {
            out.sendLine(command);

            String dbResponse = in.receiveLine();
            if (!dbResponse.equals("OK")) {
                response = WSResponse.newErrorResponse(dbResponse);
                return dbResponse;
            }

            response = WSResponse.newSuccessResponse();
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

            Map<String, Object> dbResponse = (Map<String, Object>) in.receiveObject();
            if (dbResponse.isEmpty()) {
                String errMsg = "Post non trovato";
                response = WSResponse.newErrorResponse(errMsg);
                return errMsg;
            }

            ArrayList<SimpleComment> comments = (ArrayList<SimpleComment>) dbResponse.get("COMMENTS");
            ArrayList<SimpleLike> likes = (ArrayList<SimpleLike>) dbResponse.get("LIKES");

            comments.sort(Comparator.reverseOrder());

            long nLikes = 0;
            long nDislikes = 0;

            for (SimpleLike l : likes) {
                if (l.getType() == Like.type.LIKE) {
                    nLikes++;
                } else {
                    nDislikes++;
                }
            }

            dbResponse.put("LIKES", nLikes);
            dbResponse.put("DISLIKES", nDislikes);

            String json = gson.toJson(dbResponse);

            response = WSResponse.newSuccessResponse(json);
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

            String dbResponse = in.receiveLine();
            if (!dbResponse.equals("OK")) {
                response = WSResponse.newErrorResponse(dbResponse);
                return dbResponse;
            }

            response = WSResponse.newSuccessResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "OK";
    }


}
