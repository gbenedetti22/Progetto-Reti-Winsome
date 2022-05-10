package com.unipi.database;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.unipi.common.SimpleComment;
import com.unipi.database.graph.GraphSaver;
import com.unipi.database.graph.WinsomeGraph;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;
import com.unipi.database.utility.EntriesStorage;
import com.unipi.database.utility.PriorityAsyncSaver;
import com.unipi.utility.StandardPriority;

import java.io.*;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Database implements WinsomeDatabase, Closeable {
    private WinsomeGraph graph; // grafo
    private GraphSaver graphSaver; // oggetto adibito al salvataggio del grafo
    private ConcurrentHashMap<String, User> tableUsers; // tabella degli utenti
    private ConcurrentHashMap<UUID, Post> tablePosts; // tabella dei post
    private EntriesStorage entries; // oggetto adibito al tracciamento degli oggetti (like, commenti ecc) su cui calcolare le ricompense
    private Gson gson;
    private PriorityAsyncSaver saver; // executor asincrono. Viene utilizzao to principalmente con GraphSaver

    public Database() {
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(getDateFormat().toString()).create();
        this.entries = new EntriesStorage(this);

        try {
            loadTables();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Objects.requireNonNull(tableUsers);
            Objects.requireNonNull(tablePosts);
        } catch (NullPointerException e) {
            tableUsers = new ConcurrentHashMap<>();
            tablePosts = new ConcurrentHashMap<>();
        }

        graph = new WinsomeGraph(this);
        graphSaver = new GraphSaver();
    }

    public static DatabaseDate getDateFormat() {
        return new DatabaseDate();
    }

    public static String getName() {
        return "graphDB";
    }

    public void startSaving(String delay) {
        if (!delay.matches("^[0-9]+[a-zA-Z]$")) {
            System.err.println("Formato delay non valido");
            System.err.println("Verrà usato il valore di default: ");
            delay = "5m";
        }

        this.saver = new PriorityAsyncSaver(delay);
        saver.start();

        System.out.println("Saver Avviato");
    }

    // metodo per creare un nodo utente nel grafo e attaccarci i tag passati come parametro.
    // i tag vengono controllati sul Server
    @Override
    public String createUser(String username, String password, List<String> tags) {
        if (tableUsers.containsKey(username)) return "201"; // 201 = username già presente

        ArrayList<String> tagsCopy = new ArrayList<>(5);

        for (String s : tags) {
            tagsCopy.add(s.trim());
        }

        User u = new User(username, password, tagsCopy);
        tableUsers.put(username, u);

        GraphNode<String> node = new GraphNode<>(username);

        String TAGS_LABEL = "TAGS";
        GroupNode tagsGroup = new GroupNode(TAGS_LABEL, node);

        String POSTS_LABEL = "POSTS";
        GroupNode postsGroup = new GroupNode(POSTS_LABEL, node);

        graph.putEdge(node, tagsGroup);
        graph.putEdge(node, postsGroup);

        for (String tag : tagsCopy) {
            GraphNode<String> tagNode = new GraphNode<>(tag);
            graph.putEdge(tagsGroup, tagNode);
        }

        u.setPostsGroupNode(postsGroup);
        u.setTagsGroupNode(tagsGroup);
        graphSaver.saveUser(username);  // viene creato il file sul disco. Essendo un operazione semplice, questo viene fatto subito

        return "200";
    }

    @Override
    public User getUser(String username) {
        return tableUsers.get(username);
    }

    public Post getPost(UUID idPost) {
        return tablePosts.get(idPost);
    }

    // metodo per restituire i tags di un utente.
    // Questo viene fatto se la password sul db e quella passata come parametro sono uguali.
    // Di fatto, questo metodo viene utilizzato per il login.
    @Override
    public Set<Node> getTagsIf(String username, String password) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            Set<Node> tagsSet = graph.adjacentNodes(u.getTagsGroupNode());
            if (u.getPassword().equals(password))
                // vengono restituiti i nodi utente che hanno username diverso da quello passato come parametro
                return tagsSet.stream().filter(node -> {
                    if (node instanceof GraphNode<?> g && g.getValue() instanceof String s)
                        return !s.equals(username);
                    return false;
                }).collect(Collectors.toSet());

            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }

    // metodo che restituisce gli utenti che hanno in comune al più un tag con l'utente passato come parametro.
    // Per ogni tag dell'utente, vengono presi i nodi vicini
    @Override
    public Set<Node> getUsersByTag(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            HashSet<Node> result = new HashSet<>();

            for (String tag : u.getTags()) {
                GraphNode<String> tagNode = new GraphNode<>(tag);
                Set<Node> set = graph.adjacentNodes(tagNode);
                result.addAll(set);
            }

            return Collections.unmodifiableSet(result);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public Set<String> getFollowersOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));

            return Collections.unmodifiableSet(u.getFollowers());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public Set<String> getFollowingOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return Collections.unmodifiableSet(u.getFollowing());
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public String followUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            boolean b1 = user1.addFollow(u2);
            boolean b2 = user2.addFollowers(u1);
            return b1 && b2 ? "200" : "202"; // 200 = successo, 202 = utente già seguito
        } catch (NullPointerException ignored) {
        }

        return "207"; // 207 = utente non presente
    }

    @Override
    public String unfollowUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            boolean b1 = user1.removeFollow(u2);
            boolean b2 = user2.removeFollowers(u1);
            return b1 && b2 ? "200" : "202";
        } catch (NullPointerException ignored) {
        }

        return "207";
    }

    // metodo che aggiunge un nodo Post al grafo.
    // Il post viene aggiunto anche nella tabella dei post e gli viene appeso un commentsGroupNode e un likesGroupNode
    @Override
    public Post createPost(String author, String title, String content) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(author));
            GroupNode postsGroup = u.getPostsGroupNode();

            Post p = new Post(author, title, content);
            tablePosts.put(p.getId(), p);

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

            if(saver != null)
                saver.asyncSave(() -> graphSaver.savePost(p), StandardPriority.VERY_HIGH);
            return p;
        } catch (NullPointerException e) {
            return null;
        }
    }

    // metodo per reperire un post dal database.
    // se username segue l autore del post denominato da idPost, il post viene restituito.
    // un Post può essere restituito sse l utente segue l autore del Post o se l utente è l autore del Post.
    @Override
    public HashMap<String, Object> viewFriendPost(String username, UUID idPost) {
        Post p = getPost(idPost);
        User u = getUser(username);
        if (p != null && u != null) {
            // la seconda condizione mi serve per fare in modo che l autore possa vedere il suo post
            // Di fatto, se username è l autore di idPost, lui non può seguire se stesso e quindi la prima condizione è vera;
            // al contrario, la seconda condizione sarà falsa
            if (!u.getFollowing().contains(p.getAuthor()) && !u.getUsername().equals(p.getAuthor())) {
                return new HashMap<>(0);
            }

            return postToMap(p);
        }

        return null;
    }

    // metodo per stabilire se un rewin esiste ancora oppure no.
    // Se esiste, restituisce il post originale, altrimenti null.
    @Override
    public HashMap<String, Object> openRewin(String rewinAuthor, UUID idPost) {
        User u = getUser(rewinAuthor);
        if (u != null) {
            GroupNode posts = u.getPostsGroupNode();
            if (!graph.adjacentNodes(posts).contains(new GraphNode<>(idPost))) {
                return new HashMap<>(0);
            }

            return postToMap(getPost(idPost));
        }

        return null;
    }

    // metodo per convertire un Post in un HashMap
    private HashMap<String, Object> postToMap(Post p) {
        HashMap<String, Object> post = new HashMap<>();

        GroupNode comments = p.getCommentsGroupNode();
        GroupNode likes = p.getLikesGroupNode();

        post.put("TITLE", p.getTitle());
        post.put("CONTENT", p.getContent());
        post.put("LIKES", graph.adjacentNodes(likes));
        post.put("COMMENTS", graph.adjacentNodes(comments));

        return post;
    }

    // metodo per restituire tutti i commenti dopo una certa dota.
    // Se date è 0, allora vengono restituiti tutti i commenti.
    // I commenti corrispondono ai nodi vicini al nodo commentsGroupNode del post.
    public Set<SimpleComment> getCommentsFromDate(UUID idPost, String date) throws ParseException {
        Post p = tablePosts.get(idPost);
        if (p != null) {
            GroupNode commentsGroupNode = p.getCommentsGroupNode();
            Set<Node> comments = graph.adjacentNodes(commentsGroupNode);

            HashSet<SimpleComment> set = new HashSet<>();
            if (date.equals("0")) {
                for (Node n : comments) {
                    if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                        set.add(c.toSimpleComment());
                    }
                }

                return set;
            }

            SimpleDateFormat format = Database.getDateFormat().getSimpleDateFormat();
            Date d = format.parse(date);
            for (Node n : comments) {
                if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                    if (format.parse(c.getDate()).after(d)) {
                        set.add(c.toSimpleComment());
                    }
                }
            }

            return set;
        }

        return null;
    }

    // metodo per restituire tutti i post di un utente.
    // I post corrispondono ai nodi vicini al nodo postsGroupNode dell utente.
    @Override
    public Set<Node> getAllPostsOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return graph.adjacentNodes(u.getPostsGroupNode());
        } catch (NullPointerException e) {
            return null;
        }
    }

    // metodo che restituisce tutti i post dei follow di username.
    // Di fatto, ritorna quella che è la home di username.
    @Override
    public Map<String, Set<Node>> getFriendsPostsOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            HashMap<String, Set<Node>> result = new HashMap<>(u.getFollowing().size());

            for (String follow : u.getFollowing()) {
                User friend = tableUsers.get(follow);
                result.put(follow, graph.adjacentNodes(friend.getPostsGroupNode()));
            }

            return Collections.unmodifiableMap(result);
        } catch (NullPointerException e) {
            return null;
        }
    }

    // metodo che prende in input una dateMap e restituisce tutti i post dei follow di username basandosi sulla dateMap.
    // Esempio:
    // dateMap = A | "08/08/2020 - 5.30" -> restituisce tutti i post di A che sono stati pubblicati dopo il 08/08/2020 - 5-30 (vale anche l ora non solo il giorno)
    // dateMap = B | "0" -> restituisce tutti i post di B

    // Per ogni follow di username nella dateMap, controllo se è effettivamente un follow e poi prendi i post dopo quella data
    @Override
    public Map<String, Set<Node>> getLatestFriendsPostsOf(String username, Map<String, String> dateMap) {
        User me = tableUsers.get(username);
        if (me == null) return null;

        HashMap<String, Set<Node>> result = new HashMap<>();
        SimpleDateFormat format = Database.getDateFormat().getSimpleDateFormat();

        /*
            key=username || value=date
         */
        for (Map.Entry<String, String> entry : dateMap.entrySet()) {
            String follow = entry.getKey();
            String date = entry.getValue();
            if (!me.getFollowing().contains(follow)) continue;

            try {
                User friend = Objects.requireNonNull(tableUsers.get(follow));

                if (date.equals("0")) {
                    Set<Node> nodes = graph.adjacentNodes(friend.getPostsGroupNode());
                    result.put(follow, nodes);
                    continue;
                }

                Date d;
                try {
                    d = format.parse(date);
                }catch (ParseException e) {
                    return null;
                }

                for (Node n : graph.adjacentNodes(friend.getPostsGroupNode())) {
                    if (n instanceof GraphNode<?> g && g.getValue() instanceof UUID id) {
                        Post p = tablePosts.get(id);
                        if(!p.getAuthor().equals(follow) || p.date().toDate().after(d)) {
                            result.putIfAbsent(follow, new HashSet<>());
                            result.get(follow).add(n);
                        }
                    }
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return result;
    }


    @Override
    public String rewinFriendsPost(String username, UUID idPost) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            Post p = Objects.requireNonNull(tablePosts.get(idPost));
            if (!u.getFollowing().contains(p.getAuthor())) return "205";

            GroupNode posts = u.getPostsGroupNode();
            try {
                // Se il post è già stato rewinnato, ritorna un errore
                if (graph.adjacentNodes(posts).contains(new GraphNode<>(idPost))) return "209";
            } catch (NullPointerException e) {
                e.printStackTrace();
                return "208"; // Se non ci sono post adiacebti al nodo postsGroupNode, ritorna un errore
            }

            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);
            graph.putEdge(posts, rewinNode);

            if(saver != null)
                saver.asyncSave(() -> graphSaver.saveRewin(u, idPost), StandardPriority.HIGH);

            return "200";
        } catch (NullPointerException ignored) {
        }

        return "210";
    }

    @Override
    public String removeRewin(String username, UUID idPost) {
        User u = tableUsers.get(username);
        Post p = tablePosts.get(idPost);
        if (u != null & p != null) {
            if (!u.getFollowing().contains(p.getAuthor())) return "205";

            GroupNode posts = u.getPostsGroupNode();
            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);

            graph.removeEdge(posts, rewinNode);

            if(saver != null)
                saver.asyncSave(() -> graphSaver.removeRewinFromFile(username, idPost), StandardPriority.LOW);
            return "200";
        }

        return "210";
    }

    @Override
    public Comment appendComment(UUID idPost, String author, String content) throws UnsupportedOperationException {
        User u = tableUsers.get(author);
        Post p = tablePosts.get(idPost);

//   Codice per controllare se il commento è ammissibile oppure no.
//   Il commento non è ammissibile se sto cercando di metterlo ad un Post Rewin che nessun mio following ha "rewinnato".

        if (p != null && u != null) {
            boolean ok = checkValidity(u, p);

            // Se sto cercando di commentare un post di un autore che non seguo e che nessuno ha rewinnato -> errore
            if (!ok) {
                throw new UnsupportedOperationException();
            }

            Comment c = new Comment(idPost, author, content);

            GraphNode<Comment> commentNode = new GraphNode<>(c);
            GroupNode commentsGroup = tablePosts.get(idPost).getCommentsGroupNode();

            graph.putEdge(commentsGroup, commentNode);
            entries.add(c);
            saver.asyncSave(() -> graphSaver.saveComment(tablePosts.get(idPost).getAuthor(), c), StandardPriority.NORMAL);
            return c;
        }

        return null;
    }

    @Override
    public String appendLike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.TYPE.LIKE);
    }

    @Override
    public String appendDislike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.TYPE.DISLIKE);
    }

    private boolean checkValidity(User u, Post p) {
        // Se sto agendo su un post di un mio follow, ok
        if (u.getFollowing().contains(p.getAuthor())) {
            return true;
        } else { // se sto agendo su un rewin, non per forza seguo l autore del post originale..
            for (String f1 : u.getFollowing()) {
                User followOfF1 = tableUsers.get(f1);
                // ..quindi guardo se tra tutti i miei follow, c'è qualcuno che ha rewinnato il post
                if (graph.hasEdgeConnecting(followOfF1.getPostsGroupNode(), new GraphNode<>(p.getId()))) {
                    return true;
                }
            }
        }

        return false;
    }

    // metodo per aggiungere un like o un dislike.
    // Il cambio da like a dislike è possibile in
    private String appendLikeDislike(String username, UUID idPost, Like.TYPE type) {
        User u = tableUsers.get(username);
        Post p = tablePosts.get(idPost);
        if (u == null || p == null) return "210";
        if (u.getUsername().equals(p.getAuthor())) return "211";

//   Codice per controllare se il like è ammissibile oppure no.
//   Il like non è ammissibile se sto cercando di metterlo ad un Rewin che nessun mio following ha "rewinnato".

        boolean ok = checkValidity(u, p);
        if (!ok) {
            return "217";
        }

        Post post = tablePosts.get(idPost);
        if (post == null) return "208";

        GroupNode likesGroup = post.getLikesGroupNode();

        Set<Node> likes = graph.adjacentNodes(likesGroup);

        // Eseguo un for per controllare se il like è già stato inserito.
        // Non posso usare la set.contains() perchè l hash viene fatto sull id del like (che viene assegnato quando viene creato).
        // Siccome la classe Set non permette di reperire elementi, non posso reperire l id del like già assegnato.
        // Un idea sarebbe togliere il nodo e rimetterlo,
        // ma non potendo, appunto, reperire l id del like originale non posso accedere al file sul disco per la modifica.

        // Se il cambio da like a dislike (o viceverse) viene eseguito, il calcolo delle ricompense viene fatto
        // sul nuovo like e non più su quello vecchio
        for (Node n : likes) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                if (l.getUsername().equals(username)) {
                    if (l.getType() != type) {
                        if (l.getType() == Like.TYPE.LIKE) {
                            entries.changeLikeToDislike(l);
                        } else {
                            entries.chageDislikeToLike(l);
                        }

                        l.setType(type);
                        if(saver != null)
                            saver.asyncSave(() -> graphSaver.saveLike(username, l), StandardPriority.NORMAL);
                        return "0"; //like cambiato
                    } else if (l.getType() == type)
                        return "212";
                }
            }
        }

//  Semplice controllo se il like è stato messo (alternativa al cambio Like)
//  In questo caso, like/dislike già messo = errore
//        for (Node n : likes) {
//            if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
//                if (l.getUsername().equals(username))
//                    return "212";
//            }
//        }

        Like like = new Like(idPost, type, username);
        GraphNode<Like> likeNode = new GraphNode<>(like);

        graph.putEdge(likesGroup, likeNode);
        entries.add(like);
        if(saver != null)
            saver.asyncSave(() -> graphSaver.saveLike(post.getAuthor(), like), StandardPriority.NORMAL);
        return "200";
    }


    // username vuole cancellare un post con id=idPost
    // il post può essere cancellato solo se si è l autore di esso

    // vengono rimossi tutti i commenti e like (file compresi -> vedi graphSaver.removePost())
    @Override
    public String removePost(String username, UUID idPost) {
        Post p = tablePosts.remove(idPost);
        if (p != null) {
            if (!username.equals(p.getAuthor())) return "213"; // non è l'autore del post

            GroupNode commentsGroup = p.getCommentsGroupNode();
            GroupNode likesGroup = p.getLikesGroupNode();

            Set<Node> commentSet = graph.adjacentNodes(commentsGroup);
            Set<Node> likeSet = graph.adjacentNodes(likesGroup);

            Set<Node> commentsSet = new HashSet<>(commentSet);
            for (Node node : commentsSet) {
                if (node instanceof GraphNode<?> g && g.getValue() instanceof UUID) continue;

                graph.removeNode(node);
            }

            Set<Node> likesSet = new HashSet<>(likeSet);
            for (Node node : likesSet) {
                if (node instanceof GraphNode<?> g && g.getValue() instanceof UUID) continue;

                graph.removeNode(node);
            }

            entries.remove(p.getId());
            if(saver != null)
                saver.asyncSave(() -> graphSaver.removePost(p, commentsSet, likesSet), StandardPriority.LOW);
            graph.removeNode(commentsGroup);
            graph.removeNode(likesGroup);
            graph.removeNode(new GraphNode<>(p.getId()));
            return "200";
        }

        return "208";
    }

    // metodo per reperire le entry su cui eseguire il calcolo delle ricompense
    @Override
    public ArrayList<EntriesStorage.Entry> pullNewEntries() {
        ArrayList<EntriesStorage.Entry> entries = this.entries.pull();
        for (EntriesStorage.Entry e : entries) {

            //Questi 2 for annidati, in realtà, sono come un for che cicla su tutti i commenti nuovi
            for (ArrayList<Comment> comments : e.COMMENTS.values()) {
                for (Comment c : comments) {
                    saver.asyncSave(() -> graphSaver.removeEntry(tablePosts.get(c.getIdPost()).getAuthor(), c), StandardPriority.LOW);
                }
            }

            for (Like l : e.LIKES) {
                saver.asyncSave(() -> graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l), StandardPriority.LOW);
            }

            for (Like l : e.DISLIKES) {
                saver.asyncSave(() -> graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l), StandardPriority.LOW);
            }

        }

        return entries;
    }

    public EntriesStorage getEntriesStorage() {
        return entries;
    }

    // metodo per salvare le 2 tabelle (tablePosts e tableUsers)
    public void save() {
        saveUsers();
        savePosts();
    }

    private void saveUsers() {
        try {
            String s = gson.toJson(tableUsers);
            BufferedWriter out = new BufferedWriter(new PrintWriter(getName() + File.separator + "users.json"));
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePosts() {
        try {
            String s = gson.toJson(tablePosts);
            BufferedWriter out = new BufferedWriter(new PrintWriter(getName() + File.separator + "posts.json"));
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // metodo per ricreare, mediante Gson, le 2 hashmap che contengono tutti gli utenti e i post
    private void loadTables() throws IOException {
        String usersPath = getName() + File.separator + "users.json";
        String postsPath = getName() + File.separator + "posts.json";

        File uFile = new File(usersPath);
        File pFile = new File(postsPath);
        if (!uFile.exists() || !pFile.exists()) return;

        JsonReader usersReader = new JsonReader(new FileReader(usersPath));
        JsonReader postsReader = new JsonReader(new FileReader(postsPath));
        Type tableUsersType = new TypeToken<ConcurrentHashMap<String, User>>() {
        }.getType();
        Type tablePostsType = new TypeToken<ConcurrentHashMap<UUID, Post>>() {
        }.getType();

        tableUsers = gson.fromJson(usersReader, tableUsersType);
        tablePosts = gson.fromJson(postsReader, tablePostsType);

        usersReader.close();
        postsReader.close();
    }

    public WinsomeGraph getGraph() {
        return graph;
    }

    @Override
    public void close() throws IOException {
        if (saver != null) {
            saver.interrupt();
            try {
                saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class DatabaseDate {
        private final String dateS;
        private final SimpleDateFormat dateSDF;

        public DatabaseDate() {
            dateS = "dd/MM/yy - HH:mm:ss";
            dateSDF = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
        }

        public String toString() {
            return dateS;
        }

        public SimpleDateFormat getSimpleDateFormat() {
            return dateSDF;
        }
    }
}
