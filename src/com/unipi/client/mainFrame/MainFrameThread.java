package com.unipi.client.mainFrame;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.unipi.client.*;
import com.unipi.client.UI.banners.CommentBanner;
import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.pages.*;
import com.unipi.common.*;
import com.unipi.server.RMI.FollowersService;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.*;

import static com.unipi.client.mainFrame.ClientProperties.NAMES.*;
import static com.unipi.client.mainFrame.MainFrame.showErrorMessage;
import static com.unipi.client.mainFrame.MainFrame.showSuccessMessage;
import static com.unipi.server.requestHandler.WSRequest.WS_OPERATIONS;

/*
    Classe che rappresenta il Thread principale del mainframe.
    Qua vengono fatte la maggior parte delle richieste al Server (di cui molte in modo asincrono)
 */
public class MainFrameThread extends Thread {
    private final MainFrame frame; // riferimento al MainFrame per il cambio pagina
    private final Gson gson;
    private FollowersService followersService; // interfaccia per la registrazione al servizio di follow
    private LocalStorage storage;
    private ServiceManager serviceManager; // richieste asincrone
    private HashSet<String> rewinnedPosts;

    public MainFrameThread(MainFrame frame) throws IOException, NotBoundException {
        this.frame = frame;
        this.storage = new LocalStorage();
        this.rewinnedPosts = new HashSet<>();
        setName("Main-Frame-Thread of" + frame.getName());

        HashMap<ClientProperties.NAMES, Object> props = ClientProperties.getValues();
        SocketChannel socket = SocketChannel.open(new InetSocketAddress((String) props.get(SERVER_ADDRESS), (int) props.get(DEFUALT_TCP_PORT)));
        socket.configureBlocking(true);

        Registry registry = LocateRegistry.getRegistry((String) props.get(RMI_ADDRESS), (int) props.get(RMI_FOLLOW_PORT));
        followersService = (FollowersService) registry.lookup("FOLLOWERS-SERVICE");

        this.serviceManager = new ServiceManager(socket);
        serviceManager.start();

        gson = new GsonBuilder().setDateFormat("dd/MM/yy - HH:mm:ss").create();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    System.out.println("Chiusura servizi...");
                    socket.close();
                    serviceManager.close();
                    if (storage.getCurrentUsername() != null)
                        followersService.unregister(storage.getCurrentUsername());

                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                System.out.println("Chiusura ActionPipe...");
                ActionPipe.closeActionPipe();
            }
        });
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
                case UPDATE_HOME -> downloadLatestPosts();
                case PUBLISH_ACTION -> performPublishPost();
                case PROFILE_ACTION -> performViewProfile();
                case LOGOUT_ACTION -> performLogout();
                case FOLLOWERS_PAGE_ACTION -> performViewFollowPage();
                case DISCOVER_ACTION -> performDiscover();
                case FOLLOW_ACTION -> performFollow();
                case UNFOLLOW_ACTION -> performUnfollow();
                case LIKE_ACTION -> performLike();
                case DISLIKE_ACTION -> performDislike();
                case PUBLISH_COMMENT_ACTION -> performPublishComment();
                case DELETE_POST_ACTION -> performDeletePost();
                case VIEW_POST_ACTION -> performViewPost();
                case GET_LATEST_COMMENTS -> performGetLatestComments();
                case RETWEET_ACTION -> performRewin();
                case CLOSE_ACTION -> interrupt();
            }
        }
    }

    // Metodo per reperire i commenti più recenti di un Post.
    // Questo metodo viene chiamto solo se l utente sta visualizzando un Post e clicca ripetutamente sull icona
    // dei commenti. Nel momento in cui l utente torna nella home e poi ritorna sul Post, questo viene scaricato di nuovo
    // causa mancanza di aggiornamento in real-time dei like
    private void performGetLatestComments() {
        if (frame.getCurrentPage() instanceof PostPage page) {
            CommentsPage commentsPage = (CommentsPage) ActionPipe.getParameter();

            String id = page.getId();
            String date;
            try {
                date = commentsPage.getComments().first().getDate();
            } catch (NoSuchElementException e) {
                date = "0";
            }

            WSRequest request = new WSRequest(WS_OPERATIONS.GET_COMMENTS_FROM_DATE, id, date);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            Type type = new TypeToken<TreeSet<SimpleComment>>() {
            }.getType();
            String json = response.getBody();
            Console.log(json);
            TreeSet<SimpleComment> set = gson.fromJson(json, type);
            commentsPage.addAll(set);
            commentsPage.open();
        }

    }

    // metodo per eseguire il rewin
    // la richiesta è sincrona in quanto l utente deve ricevere subito una risposta
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

    // metodo per mettere like/dislike
    // la richiesta è sincrona in quanto l utente deve ricevere una risposta visuale (il +1)
    private void performLike() {
        if (frame.getCurrentPage() instanceof PostPage page) {
            String id = page.getId();
            WSRequest request = new WSRequest(WS_OPERATIONS.LIKE, id);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            String bodyMessage = response.getBody();
            if (bodyMessage.equals("CHANGED LIKE"))
                page.setDislikeSetted(true);

            page.addLike();
        }
    }

    private void performDislike() {
        if (frame.getCurrentPage() instanceof PostPage page) {
            String id = page.getId();
            WSRequest request = new WSRequest(WS_OPERATIONS.DISLIKE, id);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            String bodyMessage = response.getBody();
            if (bodyMessage.equals("CHANGED LIKE"))
                page.setLikeSetted(true);

            page.addDislike();
        }
    }

    // metodo per pubblicare un commento
    // la richiesta è sincrona in quanto l utente deve poter vedere il commento appeso
    private void performPublishComment() {
        CommentsPage page = (CommentsPage) ActionPipe.getParameter();
        String id = page.getPost().getId();
        String commentText = page.getInputText();
        page.clearField();

        if (commentText.isBlank()) {
            showErrorMessage("Il contenuto del commento non può essere vuoto");
            return;
        }

        WSRequest request = new WSRequest(WS_OPERATIONS.COMMENT, id, commentText);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            return;
        }

        String json = response.getBody();
        SimpleComment c = gson.fromJson(json, SimpleComment.class);

        page.addComment(new CommentBanner(c.getId(), c.getAuthor(), c.getContent(), c.getDate()));
    }

    // metodo per visualizzare un Post che sta nella home o nel profilo
    // La richiesta è sincrona perchè, se l utente comincia a cliccare più post, il risultato potrebbe essere spiacevole
    // (tipo quando si tenta di aprire Chrome più volte e questo, quando si decide ad aprirsi, apre un sacco di schede)
    // se il post è un rewin, allora viene chiamato il metodo specifico
    private void performViewPost() {
        PostBanner banner = (PostBanner) ActionPipe.getParameter();
        if (banner.isRewin()) {
            performOpenRewin(banner);
            return;
        }

        String id = banner.getID();
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_POST, id);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();

        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            Pages.HOME_PAGE.removePostIf(p -> p.getID().equals(id));
            return;
        }

        String json = response.getBody();

        WrapperPost post = gson.fromJson(json, WrapperPost.class);
        PostPage page = Pages.newPostPage(id, banner.getAuthor(), post.TITLE, post.CONTENT);
        page.setLikes((int) post.LIKES);
        page.setDislikes((int) post.DISLIKES);

        for (Map<String, String> c : post.COMMENTS) {
            page.addComment(new CommentBanner(c.get("id"), c.get("author"), c.get("content"), c.get("date")));
        }

        frame.switchPage(page);
    }

    // richiesta sincrona di apertura di un rewin
    // è necessario un metodo a parte per controllare se il rewin esiste ancora oppure no
    private void performOpenRewin(PostBanner banner) {
        String id = banner.getID();
        String author = banner.getRewin();
        WSRequest request = new WSRequest(WS_OPERATIONS.OPEN_REWIN, author, id);
        serviceManager.submitRequest(request);
        WSResponse response = serviceManager.getResponse();

        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            Pages.HOME_PAGE.removePost(banner);
            return;
        }

        String json = response.getBody();

        WrapperPost post = gson.fromJson(json, WrapperPost.class);
        PostPage page = Pages.newPostPage(id, banner.getAuthor(), post.TITLE, post.CONTENT);
        page.setLikes((int) post.LIKES);
        page.setDislikes((int) post.DISLIKES);
        for (Map<String, String> c : post.COMMENTS) {
            page.addComment(new CommentBanner(c.get("id"), c.get("author"), c.get("content"), c.get("date")));
        }

        frame.switchPage(page);
    }

    // questo è un semplice cambio perchè:
    // 1) gli utenti che seguo vengono settati durante il login e poi localmente mano a mano che seguo altri utenti
    // 2) i followers sono aggiornati dal Server tramite RMI (i quali vengono aggiunti anche nella UI)
    private void performViewFollowPage() {
        frame.switchPage(Pages.FOLLOW_PAGE);
    }

    // Metodo per la cancellazione di un Post
    // Se questo era un rewin, il post corrispondente nella home viene reso rewinnabile di nuovo
    // Se si tenta di cancellare un rewin, verrà fatta una richiesta apposta
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

            if (clickedBanner.isRewin()) {
                PostBanner banner = Pages.HOME_PAGE.getPostBanner(id);
                banner.setRewinnable(true);
            }

            showSuccessMessage("Post eliminato con successo!");
        }
    }

    /*
        NOTA: quando si guardano i metodi per il follow e la unfollow,
        ci saranno dei metodi che conterranno dei nomi del tipo "left" o "right".
        Questo perchè la UI è organizzata in questo modo:
            followers | following
            ----------------------
         A destra i following e a sinistra i followers
     */

    // operazione per eseguire un follow
    // L utente, se "followato" viene aggiunto alla pagina dei follow
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
            downloadPostsOf(username);
            storage.getFollowing().add(username);
            FollowersPage page = Pages.FOLLOW_PAGE;
            // Se l utente che voglio seguire è a sua volta un mio followers,
            // allora vado nella lista dei followers e cambio l azione da "follow" a "unfollow"
            // es.
            /*
                followers         | following
               ----------------------------------------
                utenteA <follow>  |
                utenteB <follow>  |
                utenteC <follow>  |

                Se l utente clicca su "follow" dell utenteA, la schermata deve diventare così:
                followers         | following
               ----------------------------------------
                utenteA <unfollow>| utenteA <unfollow>
                utenteB <follow>  |
                utenteC <follow>  |

                Quindi devo prendere il banner da sinistra e cambiarlo in "unfollow"
             */

            FollowersPage.PageBanner banner = page.getFromLeft(username);
            if (banner != null)
                banner.doUnfollow();
            page.appendBanner(username, FollowersPage.Type.FOLLOWING);
            return;
        }

        if (frame.getCurrentPage() instanceof FollowersPage page) {
            String username = (String) ActionPipe.getParameter();

            serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.FOLLOW, username));
            WSResponse response = serviceManager.getResponse();
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            storage.getFollowing().add(username);
            page.appendBanner(username, FollowersPage.Type.FOLLOWING);
            FollowersPage.PageBanner banner = page.getFromLeft(username);
            if (banner != null)
                banner.doUnfollow();

            downloadPostsOf(username);
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
            home.removePostIf(p -> p.getAuthor().equals(username)); // rimuove tutti i post che soddisfano quella condizione
            storage.getFollowing().remove(username);
            Pages.FOLLOW_PAGE.removeFromRight(username);
            if (!home.containsPosts()) {
                home.showBackground();
            }
            return;
        }

        if (frame.getCurrentPage() instanceof FollowersPage page) {
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

            page.removeFromRight(username);
            FollowersPage.PageBanner banner = page.getFromLeft(username);
            if (banner != null)
                banner.doFollow();

            if (!home.containsPosts()) {
                home.showBackground();
            }
        }
    }

    // se non ci sono post nel profilo, viene fatto un ulteriore tentativo
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
        if (frame.getCurrentPage() instanceof LoginPage page) { // Se si è nella login page...
            String username = page.getUsername();
            String password = page.getPassword();

            boolean logged = executeLogin(username, password);
            if (logged) {
                frame.switchPage(Pages.HOME_PAGE); // cambio pagina
                frame.setTitle(frame.getTitle() + " @logged as " + username);
                setMyFollowing();   // Richiedo e setto i miei follow
                registerToFollowersService(username); // mi registro al servizio di follow e ricevo i miei followers tramite RMI
                downloadMyPosts(); // scarico i post pubblicati da me
                downloadPosts(); // scarico la mia home
                downloadTransactions(); // scarico le mie transazioni (rewards)
            }
        }
    }

    // metodo per visualizzare gli utenti che hanno in comune almeno un tag
    // la richiesta è ovviamente asincrona in quanto se gli utenti non vengono visualizzati (causa magari connessione lenta)
    // l utente deve essere comunque in grado di poter tornare nella home
    private void performDiscover() {
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_BY_TAG);
        serviceManager.submitRequest(request, response -> {
            if(!(frame.getCurrentPage() instanceof DiscoverPage)) return;

            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            Type type = new TypeToken<ArrayList<String>>() {
            }.getType();
            ArrayList<String> friends = gson.fromJson(response.getBody(), type);
            Console.log("Discover", friends);

            // Converto la lista di utenti in una lista di Banners
            // A quel punto, la aggiungo alla DiscoverPage
            ArrayList<UserBanner> banners = new ArrayList<>();
            ArrayList<String> following = storage.getFollowing();

            for (String s : friends) {
                UserBanner banner = new UserBanner(s);
                if (following.contains(s))
                    banner.setUnfollow();

                banners.add(banner);
            }

            Pages.DISCOVER_PAGE.addAll(banners);
        });

        frame.switchPage(Pages.DISCOVER_PAGE);
    }

    private void registerToFollowersService(String username) {
        try {
            boolean reg = followersService.register(username, storage);
            if (!reg) {
                showErrorMessage("Impossibile ricevere i followers a causa di un errore interno. Provare a riavviare l app");
                System.exit(0);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Metodo per pubblicare un post
    // Questo viene fatto in modo sincrono perchè l utente deve sapere subito
    // se l operazione è andata a buon fine oppure no
    private void performPublishPost() {
        if (frame.getCurrentPage() instanceof ProfilePage page) {
            String title = page.getNewPostTitle();
            String content = page.getNewPostContent();

            if (title.isBlank() || content.isBlank()) {
                showErrorMessage("I campi titolo e contenuto non possono essere vuoti!");
                return;
            }

            WSRequest request = new WSRequest(WS_OPERATIONS.CREATE_POST, title, content);
            serviceManager.submitRequest(request);
            WSResponse response = serviceManager.getResponse(); // attendo una risposta
            if (response.code() != WSResponse.CODES.OK) {
                showErrorMessage(response.getBody());
                return;
            }

            String json = response.getBody();
            SimplePost post = gson.fromJson(json, SimplePost.class);
            Console.log("Creato Post con ID: " + post.getId());

            page.addPost(new PostBanner(post, true)); // cancellabile perchè è un post che va nel profilo
            showSuccessMessage("Post creato con successo!");
        }
    }

    // la fase di logout non è altro che una grossa pulitura di tutto
    private void performLogout() {
        try {
            boolean unregistered = followersService.unregister(storage.getCurrentUsername());
            if(!unregistered) {
                showErrorMessage("C'è stato un errore interno, riprovare ad aprire l applicazione");
                System.exit(0);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        storage.clear();
        Pages.resetPages();
        serviceManager.submitRequest(new WSRequest(WS_OPERATIONS.LOGOUT));
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            MainFrame.showErrorMessage("C'è stato un errore nel disconnettersi dal Server");
        }

        serviceManager.reconnect();
        frame.setTitle("Winsome - Social Network");
        frame.switchPage(Pages.LOGIN_PAGE);
    }

    // Metodo per la fase di login
    // Restituisce true se avviene il login
    // E' qua che ricevo le informazioni sul multicast e i tags
    private boolean executeLogin(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            showErrorMessage("I campi username e password non possono essere vuoti");
            return false;
        }

        WSRequest loginRequest = new WSRequest(WSRequest.WS_OPERATIONS.LOGIN, username, password);
        serviceManager.submitRequest(loginRequest);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            return false;
        }

        String json = response.getBody();
        LoginResponse logResponse = gson.fromJson(json, LoginResponse.class);
        Console.log(logResponse);

        ClientProperties.setMulticastAddress(logResponse.getMulticastAddress());
        ClientProperties.setMulticastPort(logResponse.getMulticastPort());
        MulticastClient multicastThread = new MulticastClient(logResponse.getMulticastAddress(), logResponse.getMulticastPort(), serviceManager, storage);
        multicastThread.start();

        System.out.println("MultivastClient avviato!");
        Pages.HOME_PAGE.setTags(logResponse.getTags());

        storage.setCurrentUsername(username);
        Pages.PROFILE_PAGE.setUsername(username);
        return true;
    }

    private void downloadTransactions() {
        WSRequest transactionsRequest = new WSRequest(WS_OPERATIONS.GET_TRANSACTIONS);
        serviceManager.submitRequest(transactionsRequest);
        WSResponse response = serviceManager.getResponse();
        if (response.code() != WSResponse.CODES.OK) {
            showErrorMessage(response.getBody());
            return;
        }

        Type type = new TypeToken<LinkedList<WinsomeTransaction>>() {
        }.getType();
        LinkedList<WinsomeTransaction> transactions = gson.fromJson(response.getBody(), type);
        Pages.PROFILE_PAGE.setTransactions(transactions);
    }

    // Metodo per settare i follow
    // La richiesta è sincrona perchè poi il download dei post si basa tutto su, appunto, i follow
    // Quindi, nel momento in cui voglio vedere i post della gente che seguo, questi devo averli tutti
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
        for (String follow : storage.getFollowing()) {
            //questi vanno nella sezione a destra -> posso solo unfollow
            Pages.FOLLOW_PAGE.appendBanner(follow, FollowersPage.Type.FOLLOWING);
        }
    }

    // metodi per il download dei post che vanno nella home e nel profilo
    // entrambe le richieste sono asincrone
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

                if (rewinnedPosts.contains(p.getId())) {
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

        Console.log("DateMap: " + dateMap);
        WSRequest request = new WSRequest(WS_OPERATIONS.GET_FRIENDS_POST_FROM_DATE, gson.toJson(dateMap));

        serviceManager.submitRequest(request, response -> {
            Type type = new TypeToken<ArrayList<SimplePost>>() {
            }.getType();
            ArrayList<SimplePost> posts = gson.fromJson(response.getBody(), type);
            Console.log("Post degli amici: " + posts, "(Data)");
            if (posts.isEmpty()) {
                return;
            }

            List<PostBanner> postBanners = posts.stream().map(simplePost -> {
                PostBanner banner = new PostBanner(simplePost);
                if (simplePost.isRewinned()) {
                    banner.setAsRewin(simplePost.getRewin());
                    banner.setRewinnable(false);
                    return banner;
                }

                if (rewinnedPosts.contains(simplePost.getId())) {
                    banner.setRewinnable(false);
                }

                banner.setRewinnable(true);
                return banner;
            }).toList();

            HomePage home = Pages.HOME_PAGE;
            home.addAll(postBanners);
        });
    }

    // metodo per scaricare tutti i post di un utente
    // viene chiamato nel momento in cui comincio a seguire un nuovo utente
    private void downloadPostsOf(String username) {
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

                if (rewinnedPosts.contains(simplePost.getId())) {
                    banner.setRewinnable(false);
                }

                banner.setRewinnable(true);
                return banner;
            }).toList();

            HomePage home = Pages.HOME_PAGE;
            home.addAll(postBanners);
        });
    }

    // metodo per la costruzione della dateMap
    // Il procedimento è il seguente:
    // di base, voglio tutti post di tutti gli utenti (per questo il primo for dove metto 0)
    // ciclo sui post nella home e aggiorno lo 0 con la data dell ultimo post ricevuto da quell utente specifico
    // future implementazioni, possono migliorare anche la ricezione dei rewin
    private HashMap<String, String> getDateMap() {
        Set<PostBanner> banners = Pages.HOME_PAGE.getBanners();
        HashMap<String, String> dateMap = new HashMap<>();

        for (String follow : storage.getFollowing()) {
            dateMap.put(follow, "0");
        }

        if (banners.isEmpty() && storage.getFollowing().isEmpty())
            return null;

        if (banners.isEmpty() && !storage.getFollowing().isEmpty()) {
            return dateMap;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");

        for (PostBanner p : banners) {
            if (p.isRewin()) continue;

            String previousDate = dateMap.get(p.getAuthor());
            if (previousDate.equals("0")) {
                dateMap.put(p.getAuthor(), p.getDate());
                continue;
            }

            try {
                Date d1 = sdf.parse(p.getDate());
                Date d2 = sdf.parse(previousDate);
                if (d2.before(d1))
                    dateMap.put(p.getAuthor(), p.getDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        return dateMap;
    }

}
