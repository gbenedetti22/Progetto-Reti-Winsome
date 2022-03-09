package com.unipi.client.mainFrame;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unipi.client.LocalStorage;
import com.unipi.client.Pages;
import com.unipi.client.ServiceManager;
import com.unipi.client.UI.banners.CommentBanner;
import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.pages.*;
import com.unipi.common.Console;
import com.unipi.common.SimplePost;
import com.unipi.server.RMI.FollowersService;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static com.unipi.client.mainFrame.ClientProperties.NAMES.*;
import static com.unipi.server.requestHandler.WSRequest.WS_OPERATIONS;

public class MainFrameThread extends Thread {
    private final MainFrame frame;
    private final Gson gson;
    private SocketChannel socket;
    private FollowersService followersService;
    private LocalStorage storage;
    private ServiceManager serviceManager;
    private HashSet<String> rewinnedPosts;

    public MainFrameThread(MainFrame frame) throws IOException, NotBoundException {
        this.frame = frame;
        this.storage = new LocalStorage();
        this.rewinnedPosts = new HashSet<>();
        setName("Main-Frame-Thread");

        HashMap<ClientProperties.NAMES, Object> props = ClientProperties.getValues();
        socket = SocketChannel.open(new InetSocketAddress((String) props.get(SERVER_ADDRESS), (int) props.get(DEFUALT_TCP_PORT)));
        socket.configureBlocking(true);

        Registry registry = LocateRegistry.getRegistry((String) props.get(RMI_ADDRESS), (int) props.get(RMI_FOLLOW_PORT));
        followersService = (FollowersService) registry.lookup("FOLLOWERS-SERVICE");

        this.serviceManager = new ServiceManager(socket);
        serviceManager.start();

        gson = new GsonBuilder().setDateFormat("dd/MM/yy - hh:mm:ss").create();
    }

    @Override
    public void run() {

        while (!interrupted()) {
            ACTIONS action = ActionPipe.waitForAction();

            switch (action) {
                case LOGIN_ACTION -> performLoginActon();
                case REGISTER_ACTION -> performRegisterAction();
                case BACKPAGE_ACTION -> frame.goBack();
                case SWITCH_PAGE -> {
                    if (ActionPipe.getParameter() instanceof JPanel page) {
                        frame.switchPage(page);
                        if (page instanceof HomePage)
                            downloadLatestPosts();
                    }
                }
                case PUBLISH_ACTION -> performPublishPost();
                case PROFILE_ACTION -> performViewProfile();
                case LOGOUT_ACTION -> {
                }
                case FOLLOWERS_PAGE_ACTION -> performViewFollowPage();
                case DISCOVER_ACTION -> performDiscover();
                case FOLLOW_ACTION -> performFollow();
                case UNFOLLOW_ACTION -> performUnfollow();
                case LIKE_ACTION -> performLike();
                case DISLIKE_ACTION -> performDislike();
                case PUBLISH_COMMENT_ACTION -> performPublishComment();
                case DELETE_POST_ACTION -> performDeletePost();
                case VIEW_POST_ACTION -> performViewPost();
                case RETWEET_ACTION -> performRewin();
                case CLOSE_ACTION -> {
                }
            }
        }
    }

    private void performRewin() {
        if (frame.getCurrentPage() instanceof HomePage) {
            PostBanner banner = (PostBanner) ActionPipe.getParameter();
            String id = banner.getID();
            WSRequest request = new WSRequest(WS_OPERATIONS.REWIN, id);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            banner.setRewinnable(false);
            PostBanner b = new PostBanner(banner.getAttachedPost(), true);
            b.setAsRewin(storage.getCurrentUsername());
            Pages.PROFILE_PAGE.addPost(b);
            showSuccessMessage("Post rewinnato!");
        }
    }

    private void performDislike() {
        if(frame.getCurrentPage() instanceof PostPage page) {
            String id = page.getId();
            WSRequest request = new WSRequest(WS_OPERATIONS.DISLIKE, id);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            page.addDislike();
        }
    }

    private void performLike() {
        if(frame.getCurrentPage() instanceof PostPage page) {
            String id = page.getId();
            WSRequest request = new WSRequest(WS_OPERATIONS.LIKE, id);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            page.addLike();
        }
    }

    private void performPublishComment() {
        CommentsPage page = (CommentsPage) ActionPipe.getParameter();
        String id = page.getPost().getId();
        String comment = page.getInputText();
        page.clearField();

        if (comment.isBlank()) {
            showErrorMessage("Il contenuto del commento non può essere vuoto");
            return;
        }

        WSRequest request = new WSRequest(WS_OPERATIONS.COMMENT, id, comment);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            return;
        }

        page.addComment(new CommentBanner(storage.getCurrentUsername(), comment));
    }

    @SuppressWarnings("unchecked")
    private void performViewPost() {
        String id = (String) ActionPipe.getParameter();
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_POST, id);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage("Impossibile visualizzare questo Post! Probabilmente è stato cancellato");
            return;
        }

        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = gson.fromJson(response.getBody(), type);

        String title = (String) map.get("TITLE");
        String content = (String) map.get("CONTENT");
        PostPage page = Pages.newPostPage(id, title, content);

        List<Map<String, String>> comments = (List<Map<String, String>>) map.get("COMMENTS");
        int likes = (int) ((double) map.get("LIKES"));
        int dislikes = (int) ((double) map.get("DISLIKES"));

        for (Map<String, String> c : comments) {
            page.addComment(new CommentBanner(c.get("author"), c.get("content")));
        }

        Console.log(map);
        page.setLikes(likes);
        page.setDislikes(dislikes);
        frame.switchPage(page);
    }

    private void performViewFollowPage() {
        FollowersPage page = Pages.FOLLOW_PAGE;
        for (String follow : storage.getFollowing()) {
            page.appendBanner(follow, FollowersPage.Type.FOLLOWING);
        }

        for (String followers : storage.getFollowers()) {
            page.appendBanner(followers, FollowersPage.Type.FOLLOWING);
        }

        frame.switchPage(page);
    }

    private void performDeletePost() {
        if (frame.getCurrentPage() instanceof ProfilePage profile) {
            PostBanner clickedBanner = (PostBanner) ActionPipe.getParameter();
            String id = clickedBanner.getID();
            WSRequest request;
            if (clickedBanner.isRewin()) {
                request = new WSRequest(WS_OPERATIONS.REMOVE_REWIN, id);
            } else {
                request = new WSRequest(WS_OPERATIONS.REMOVE_POST, id);
            }

            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            Console.log(response.getBody());
            profile.removePost(clickedBanner);

            if(clickedBanner.isRewin()) {
                PostBanner banner = Pages.HOME_PAGE.getPostBanner(id);
                if(clickedBanner.getRewin().equals(storage.getCurrentUsername())) {
                    Pages.HOME_PAGE.removePost(banner);
                }else {
                    banner.setRewinnable(true);
                }
            }
            showSuccessMessage("Post eliminato con successo!");
        }
    }

    private void performFollow() {
        if (frame.getCurrentPage() instanceof DiscoverPage) {
            UserBanner clickedBanner = (UserBanner) ActionPipe.getParameter();
            String username = clickedBanner.getUsername();

            serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.FOLLOW, username));
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            clickedBanner.setUnfollow();
            downloadPostOf(username);
            storage.getFollowing().add(username);
            return;
        }

        if (frame.getCurrentPage() instanceof FollowersPage) {
            String username = (String) ActionPipe.getParameter();

            serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.FOLLOW, username));
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            storage.getFollowing().add(username);
            downloadPostOf(username);
        }
    }

    private void performUnfollow() {
        if (frame.getCurrentPage() instanceof DiscoverPage) {
            UserBanner clickedBanner = (UserBanner) ActionPipe.getParameter();
            String username = clickedBanner.getUsername();

            serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.UNFOLLOW, username));
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            clickedBanner.setFollow();
            HomePage home = Pages.HOME_PAGE;
            home.removePostIf(p -> p.getAuthor().equals(username));
            storage.getFollowing().remove(username);
            if (!home.containsPosts()) {
                home.showBackground();
            }
        }

        if (frame.getCurrentPage() instanceof FollowersPage) {
            String username = (String) ActionPipe.getParameter();

            serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.UNFOLLOW, username));
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            HomePage home = Pages.HOME_PAGE;
            home.removePostIf(p -> p.getAuthor().equals(username));
            storage.getFollowing().remove(username);
            if (!home.containsPosts()) {
                home.showBackground();
            }
        }
    }


    private void performViewProfile() {
        frame.switchPage(Pages.PROFILE_PAGE);

        if (!Pages.PROFILE_PAGE.containsPost())
            downloadMyPosts();
    }

    private void performRegisterAction() {
        //la registrazione tramite RMI viene fatta internamente
        RegisterPage page = Pages.REGISTER_PAGE;
        page.clearFields();

        frame.switchPage(page);
    }

    private void performLoginActon() {
        if (frame.getCurrentPage() instanceof LoginPage page) {
            String username = page.getUsername();
            String password = page.getPassword();

            boolean logged = executeLogin(username, password);
            if (logged) {
                registerToRegistrationService(username);
                frame.switchPage(Pages.HOME_PAGE);
                setMyFollowing();
                downloadMyPosts();
                downloadPosts();
            }
        }
    }

    private void performDiscover() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_BY_TAG);
        serviceManager.submitRequest(request);

        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            return;
        }

        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();
        ArrayList<String> friends = gson.fromJson(response.getBody(), type);
        Console.log("Discover", friends);

        ArrayList<UserBanner> banners = new ArrayList<>();
        ArrayList<String> following = storage.getFollowing();

        for (String s : friends) {
            UserBanner banner = new UserBanner(s);
            if (following.contains(s))
                banner.setUnfollow();

            banners.add(banner);
        }

        Pages.DISCOVER_PAGE.addAll(banners);
        frame.switchPage(Pages.DISCOVER_PAGE);
    }

    private void registerToRegistrationService(String username) {
        try {
            boolean reg = followersService.register(username, storage);
            if (!reg) {
                showErrorMessage("Impossibile registrarsi al servizio di follow");
                System.exit(0);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void performPublishPost() {
        if (frame.getCurrentPage() instanceof ProfilePage page) {
            String title = page.getNewPostTitle();
            String content = page.getNewPostContent();

            WSRequest request = new WSRequest(WS_OPERATIONS.CREATE_POST, title, content);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            String json = response.getBody();
            SimplePost post = gson.fromJson(json, SimplePost.class);
            Console.log("Creato Post con ID: " + post.getId());
            page.addPost(new PostBanner(post, true));
            showSuccessMessage("Post creato con successo!");
        }
    }


    private void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(null,
                msg,
                "Errore", JOptionPane.ERROR_MESSAGE);
    }

    private void showSuccessMessage(String msg) {
        ImageIcon icon = new ImageIcon("./resources/checked.png");
        Image image = icon.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);

        JOptionPane.showMessageDialog(null, msg, "", JOptionPane.PLAIN_MESSAGE, icon);
    }

    private boolean executeLogin(String username, String password) {
        WSRequest loginRequest = new WSRequest(WSRequest.WS_OPERATIONS.FIND_USER, username, password);
        serviceManager.submitRequest(loginRequest);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage("Credenziali inserite errate");
            return false;
        }

        String jsonTags = response.getBody();
        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        ArrayList<String> tags = gson.fromJson(jsonTags, listType);
        Pages.HOME_PAGE.setTags(tags);
        storage.setCurrentUsername(username);
        Pages.PROFILE_PAGE.setUsername(username);
        return true;
    }

    private void setMyFollowing() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FOLLOWING);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();

        String myFollowing = response.getBody();
        Type type = new TypeToken<ArrayList<String>>() {
        }.getType();

        Console.log("Follow ricevuti: " + myFollowing);

        ArrayList<String> following = gson.fromJson(myFollowing, type);
        storage.setFollowing(following);
    }

    private void downloadPosts() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_POSTS);
        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            HomePage home = Pages.HOME_PAGE;
            if (home.containsPosts())
                home.clear();

            Console.log("Post degli amici: " + posts);
            for (SimplePost p : posts) {
                PostBanner banner = new PostBanner(p);
                banner.setRewinnable(true);
                if (p.isRewinned()) {
                    banner.setAsRewin(p.getRewin());
                    banner.setRewinnable(false);
                }

                if(rewinnedPosts.contains(p.getId())) {
                    banner.setRewinnable(false);
                }

                home.addPost(banner);
            }
        });
    }

    private void downloadMyPosts() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_MY_POSTS);
        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            ProfilePage profile = Pages.PROFILE_PAGE;

            Console.log("Post miei ricevuti: " + posts);
            for (SimplePost p : posts) {
                PostBanner banner = new PostBanner(p);
                if (p.isRewinned()) {
                    banner.setAsRewin(storage.getCurrentUsername());
                    rewinnedPosts.add(p.getId());
                }

                banner.setDeletable();
                profile.addPost(banner);
            }
        });
    }

    private void downloadLatestPosts() {
        HashMap<String, String> dateMap = getDateMap();
        if (dateMap == null) return;

        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_POST_FROM_DATE, gson.toJson(dateMap));

        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            if (posts.isEmpty()) {
                Console.log("[] (Data)");
                return;
            }

            Console.log("Post degli amici: " + posts, "(Data)");
            List<PostBanner> postBanners = posts.stream().map(simplePost -> {
                PostBanner banner = new PostBanner(simplePost);
                if (simplePost.isRewinned()) {
                    banner.setAsRewin(simplePost.getRewin());
                    banner.setRewinnable(false);
                    return banner;
                }

                if(rewinnedPosts.contains(simplePost.getId())) {
                    banner.setRewinnable(false);
                }

                banner.setRewinnable(true);
                return banner;
            }).toList();

            HomePage home = Pages.HOME_PAGE;
            home.addAll(postBanners);
        });
    }

    private void downloadPostOf(String username) {
        HashMap<String, String> dateMap = new HashMap<>();
        dateMap.put(username, "0");
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_POST_FROM_DATE, gson.toJson(dateMap));

        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            if (posts.isEmpty()) {
                Console.log("[]");
                return;
            }
            Console.log("Post degli amici: " + posts, username);

            List<PostBanner> postBanners = posts.stream().map(simplePost -> {
                PostBanner banner = new PostBanner(simplePost);
                if (simplePost.isRewinned()) {
                    banner.setAsRewin(simplePost.getRewin());
                    banner.setRewinnable(false);
                    return banner;
                }

                if(rewinnedPosts.contains(simplePost.getId())) {
                    banner.setRewinnable(false);
                }

                banner.setRewinnable(true);
                return banner;
            }).toList();

            HomePage home = Pages.HOME_PAGE;
            home.addAll(postBanners);
        });
    }

    private HashMap<String, String> getDateMap() {
        Set<PostBanner> banners = Pages.HOME_PAGE.getBanners();
        if (banners.isEmpty())
            return null;

        HashMap<String, String> dateMap = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy - hh:mm:ss");

        for (PostBanner p : banners) {
            String previousDate = dateMap.putIfAbsent(p.getAuthor(), p.getDate());
            if (previousDate != null) {
                try {
                    Date d1 = sdf.parse(p.getDate());
                    Date d2 = sdf.parse(previousDate);
                    if (d2.before(d1))
                        dateMap.put(p.getAuthor(), p.getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return dateMap;
    }
}
