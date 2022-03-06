package com.unipi.client.mainFrame;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.unipi.client.LocalStorage;
import com.unipi.client.Pages;
import com.unipi.client.ServiceManager;
import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.pages.*;
import com.unipi.server.RMI.FollowersService;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;
import com.unipi.utility.channelsio.ChannelReceiver;
import com.unipi.utility.channelsio.ChannelSender;
import com.unipi.utility.common.SimplePost;

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
import java.util.ArrayList;
import java.util.HashMap;

import static com.unipi.client.mainFrame.ClientProperties.NAMES.*;
import static com.unipi.server.requestHandler.WSRequest.WS_OPERATIONS;

public class MainFrameThread extends Thread {
    private final MainFrame frame;
    private final Gson gson;
    private SocketChannel socket;
    private ArrayList<String> tags;
    private HomePage home;
    private ProfilePage profile;
    private ChannelSender out;
    private ChannelReceiver in;
    private FollowersService followersService;
    private LocalStorage storage;
    private ServiceManager serviceManager;

    public MainFrameThread(MainFrame frame) throws IOException, NotBoundException {
        this.frame = frame;
        this.storage = new LocalStorage();
        setName("Main-Frame-Thread");

        HashMap<ClientProperties.NAMES, Object> props = ClientProperties.getValues();
        socket = SocketChannel.open(new InetSocketAddress((String) props.get(SERVER_ADDRESS), (int) props.get(DEFUALT_TCP_PORT)));
        socket.configureBlocking(true);

        Registry registry = LocateRegistry.getRegistry((String) props.get(RMI_ADDRESS), (int) props.get(RMI_FOLLOW_PORT));
        followersService = (FollowersService) registry.lookup("FOLLOWERS-SERVICE");

        out = new ChannelSender(socket);
        in = new ChannelReceiver(socket);
        this.serviceManager = new ServiceManager(socket);
        serviceManager.start();

        gson = new Gson();
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
                    }
                }
                case PUBLISH_ACTION -> performPublishPost();
                case PROFILE_ACTION -> performViewProfile();
                case HOME_ACTION -> {
                }
                case LOGOUT_ACTION -> {
                }
                case FOLLOWERS_PAGE_ACTION -> {
                }
                case DISCOVER_ACTION -> performDiscover();
                case FOLLOW_ACTION -> {
                }
                case UNFOLLOW_ACTION -> {
                }
                case LIKE_ACTION -> {
                }
                case DISLIKE_ACTION -> {
                }
                case PUBLISH_COMMENT_ACTION -> {
                }
                case DELETE_POST_ACTION -> {
                }
                case VIEW_POST_ACTION -> {
                }
                case RETWEET_ACTION -> {
                }
                case NONE -> {
                }
                case CLOSE_ACTION -> {
                }
            }
        }
    }


    private void performViewProfile() {
        frame.switchPage(Pages.PROFILE_PAGE);
        downloadMyPosts();
    }

    private void performRegisterAction() {
        //la registrazione tramite RMI viene fatta internamente
        RegisterPage page = new RegisterPage();
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
                downloadPosts();
            }
        }
    }

    private void performDiscover() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_BY_TAG);
        serviceManager.submitRequest(request);

        WSResponse response = serviceManager.getResponse();
        if(response.status() != WSResponse.S_STATUS.OK) {
            showErrorMessage(response.getBody());
            return;
        }

        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        ArrayList<String> friends = gson.fromJson(response.getBody(), type);
        System.out.println(friends);
        
        ArrayList<UserBanner> banners = new ArrayList<>();
        for (String s : friends) {
            banners.add(new UserBanner(s));
        }

        DiscoverPage page = Pages.newDiscoverPage(banners);
        frame.switchPage(page);
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
            if(response.status() != WSResponse.S_STATUS.OK){
                showErrorMessage(response.getBody());
                return;
            }

            String json = response.getBody();
            SimplePost post = gson.fromJson(json, SimplePost.class);
            System.out.println("Creato Post con ID: " + post.getId());
            page.addPost(new PostBanner(post));
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
        if (response.status() != WSResponse.S_STATUS.OK) {
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

        System.out.println("Follow ricevuti: " + myFollowing);

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

            System.out.println("Post degli amici: " + posts);
            for (SimplePost p : posts) {
                home.addPost(new PostBanner(p));
            }
        });
    }

    private void downloadMyPosts() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_MY_POSTS);
        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            ProfilePage home = Pages.PROFILE_PAGE;

            System.out.println("Post miei ricevuti: " + posts);
            for (SimplePost p : posts) {
                PostBanner banner = new PostBanner(p);
                banner.setDeletable();

                home.addPost(banner);
            }
        });
    }
}
