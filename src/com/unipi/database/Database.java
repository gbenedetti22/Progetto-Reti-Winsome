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
import com.unipi.database.utility.PriorityAsyncExecutor;
import com.unipi.utility.StandardPriority;

import java.io.*;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Database implements WinsomeDatabase, Closeable {
    private WinsomeGraph graph;
    private GraphSaver graphSaver;
    private ConcurrentHashMap<String, User> tableUsers;
    private ConcurrentHashMap<UUID, Post> tablePosts;
    private EntriesStorage entries;
    private Gson gson;
    private PriorityAsyncExecutor saver;

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

        this.saver = new PriorityAsyncExecutor(delay);
        saver.start();

        System.out.println("Saver Avviato");
    }

    @Override
    public String createUser(String username, String password, List<String> tags) {
        if (tableUsers.containsKey(username)) return "201";

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
        graphSaver.saveUser(username);

        return "200";
    }

    @Override
    public User getUser(String username) {
        return tableUsers.get(username);
    }

    @Override
    public Set<Node> getTagsIf(String username, String password) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            Set<Node> tagsSet = graph.adjacentNodes(u.getTagsGroupNode());
            if (u.getPassword().equals(password))
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

    //u1 segue u2
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
            return b1 && b2 ? "200" : "202";
        } catch (NullPointerException ignored) {
        }

        return "207";
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

            u.setDateOfLastPost(p.date().toDate());

            saver.asyncExecute(() -> graphSaver.savePost(p), StandardPriority.VERY_HIGH);
            return p;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Post getPost(UUID idPost) {
        return tablePosts.get(idPost);
    }

    @Override
    public HashMap<String, Object> viewFriendPost(String username, UUID idPost) {
        Post p = getPost(idPost);
        User u = getUser(username);
        if (p != null && u != null) {
            if (!u.getFollowing().contains(p.getAuthor()) && !u.getUsername().equals(p.getAuthor())) {
                return new HashMap<>(0);
            }

            return postToMap(p);
        }

        return null;
    }

    //metodo per stabilire se un rewin esiste ancora oppure no
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

    public Set<SimpleComment> getCommentsFromDate(UUID idPost, String date) throws ParseException {
        Post p = tablePosts.get(idPost);
        if(p != null) {
            GroupNode commentsGroupNode = p.getCommentsGroupNode();
            Set<Node> comments = graph.adjacentNodes(commentsGroupNode);

            HashSet<SimpleComment> set = new HashSet<>();
            if(date.equals("0")) {
                for (Node n : comments) {
                    if(n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                        set.add(c.toSimpleComment());
                    }
                }

                return set;
            }

            SimpleDateFormat format = Database.getDateFormat().getSimpleDateFormat();
            Date d = format.parse(date);
            for (Node n : comments) {
                if(n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                    if(format.parse(c.getDate()).after(d)) {
                        set.add(c.toSimpleComment());
                    }
                }
            }

            return set;
        }

        return null;
    }

    @Override
    public Set<Node> getAllPostsOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return graph.adjacentNodes(u.getPostsGroupNode());
        } catch (NullPointerException e) {
            return null;
        }
    }

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

    @Override
    public Map<String, Set<Node>> getLatestFriendsPostsOf(String username, Map<String, String> dateMap) {
        User me = tableUsers.get(username);
        if (me == null) return null;

        HashMap<String, Set<Node>> result = new HashMap<>();
        SimpleDateFormat format = Database.getDateFormat().getSimpleDateFormat();

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
                //se la amico ha postato qualcosa dopo la data d
                if (friend.getDateOfLastPost().after(d)) {
                    Set<Node> nodes = graph.adjacentNodes(friend.getPostsGroupNode());
                    Set<Node> postsAfterDate = nodes.stream()
//                            .parallel()
                            .filter(node -> {
                                if (node instanceof GraphNode<?> g && g.getValue() instanceof UUID id) {
                                    Post p = tablePosts.get(id);
                                    return p.date().toDate().after(d);
                                }

                                return false;
                            }).collect(Collectors.toSet());

                    result.put(follow, postsAfterDate);
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
                if (graph.adjacentNodes(posts).contains(new GraphNode<>(idPost))) return "209";
            } catch (NullPointerException e) {
                e.printStackTrace();
                return "208";
            }

            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);
            graph.putEdge(posts, rewinNode);
            saver.asyncExecute(() -> graphSaver.saveRewin(u, idPost), StandardPriority.HIGH);
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
            saver.asyncExecute(() -> graphSaver.removeRewinFromFile(username, idPost), StandardPriority.LOW);
            return "200";
        }

        return "210";
    }

    @Override
    public Comment appendComment(UUID idPost, String author, String content) throws UnsupportedOperationException {
        User u = tableUsers.get(author);
        Post p = tablePosts.get(idPost);

//   Codice per controllare se il commento è ammissibile oppure no.
//   Il commento non è ammissibile se sto cercando di metterlo ad un Rewin che nessun mio following ha "rewinnato".

        if (p != null && u != null) {

            boolean ok = false;
            if(u.getFollowing().contains(p.getAuthor())) {
                ok = true;
            } else {
                for (String f1 : u.getFollowing()) {
                    User followOfF1 = tableUsers.get(f1);
                    if(graph.hasEdgeConnecting(followOfF1.getPostsGroupNode(), new GraphNode<>(idPost))) {
                        ok = true;
                        break;
                    }
                }
            }

            if(!ok) {
                throw new UnsupportedOperationException();
            }

            Comment c = new Comment(idPost, author, content);

            GraphNode<Comment> commentNode = new GraphNode<>(c);
            GroupNode commentsGroup = tablePosts.get(idPost).getCommentsGroupNode();

            graph.putEdge(commentsGroup, commentNode);
            entries.add(c);
            saver.asyncExecute(() -> graphSaver.saveComment(tablePosts.get(idPost).getAuthor(), c), StandardPriority.NORMAL);
            return c;
        }

        return null;
    }

    @Override
    public String appendLike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.type.LIKE);
    }

    @Override
    public String appendDislike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.type.DISLIKE);
    }

    private String appendLikeDislike(String username, UUID idPost, Like.type type) {
        User u = tableUsers.get(username);
        Post p = tablePosts.get(idPost);
        if (u == null || p == null) return "210";
        if (u.getUsername().equals(p.getAuthor())) return "211";

//   Codice per controllare se il like è ammissibile oppure no.
//   Il like non è ammissibile se sto cercando di metterlo ad un Rewin che nessun mio following ha "rewinnato".

        boolean ok = false;
        if(u.getFollowing().contains(p.getAuthor())) {
            ok = true;
        } else {
            for (String f1 : u.getFollowing()) {
                User followOfF1 = tableUsers.get(f1);
                if(graph.hasEdgeConnecting(followOfF1.getPostsGroupNode(), new GraphNode<>(idPost))) {
                    ok = true;
                    break;
                }
            }
        }
        if(!ok) {
            return "217";
        }

        Post post = tablePosts.get(idPost);
        if (post == null) return "208";

        GroupNode likesGroup = post.getLikesGroupNode();

        Set<Node> likes = graph.adjacentNodes(post.getLikesGroupNode());
        for (Node n : likes) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                if (l.getUsername().equals(username)) {
                    if (l.getType() != type) {
                        if(l.getType() == Like.type.LIKE) {
                            entries.changeLikeToDislike(l);
                        }else {
                            entries.chageDislikeToLike(l);
                        }

                        l.setType(type);
                        saver.asyncExecute(() -> graphSaver.saveLike(username, l), StandardPriority.NORMAL);
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
        graphSaver.saveLike(post.getAuthor(), like);
        return "200";
    }


    //username vuole cancellare un post con id=idPost
    @Override
    public String removePost(String username, UUID idPost) {
        Post p = tablePosts.remove(idPost);
        if (p != null) {
            if (!username.equals(p.getAuthor())) return "213";

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
            saver.asyncExecute(() -> graphSaver.removePost(p, commentsSet, likesSet), StandardPriority.LOW);
            graph.removeNode(commentsGroup);
            graph.removeNode(likesGroup);
            graph.removeNode(new GraphNode<>(p.getId()));
            return "200";
        }

        return "208";
    }

    @Override
    public ArrayList<EntriesStorage.Entry> pullNewEntries() {
        ArrayList<EntriesStorage.Entry> entries = this.entries.pull();
        for (EntriesStorage.Entry e : entries) {

            //Questi 2 for annidati, in realtà, sono come un for che cicla su tutti i commenti nuovi
            for (ArrayList<Comment> comments : e.COMMENTS.values()) {
                for (Comment c : comments) {
                    saver.asyncExecute(() -> graphSaver.removeEntry(tablePosts.get(c.getIdPost()).getAuthor(), c), StandardPriority.LOW);
                }
            }

            for (Like l : e.LIKES) {
                saver.asyncExecute(() -> graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l), StandardPriority.LOW);
            }

            for (Like l : e.DISLIKES) {
                saver.asyncExecute(() -> graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l), StandardPriority.LOW);
            }

        }

        return entries;
    }

    public EntriesStorage getEntriesStorage() {
        return entries;
    }

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
