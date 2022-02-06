package com.unipi.database;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.unipi.database.graph.GraphSaver;
import com.unipi.database.graph.WinsomeGraph;
import com.unipi.database.graph.graphNodes.GraphNode;
import com.unipi.database.graph.graphNodes.GroupNode;
import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Like;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WinsomeDatabase {
    private WinsomeGraph graph;
    private GraphSaver graphSaver;
    private ConcurrentHashMap<String, User> tableUsers;
    private ConcurrentHashMap<UUID, Post> tablePosts;
    private GroupNode newEntryGroup;
    private Gson gson;

    public WinsomeDatabase() {
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat("dd/MM/yy - hh:mm:ss").create();

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

        String NEW_ENTRY_LABEL = "NEW ENTRY";
        GroupNode newEntryGroup = new GroupNode(NEW_ENTRY_LABEL, null);
        this.newEntryGroup = newEntryGroup;
        graph.addNode(newEntryGroup);
    }

    public void createUser(String username, String password, List<String> tags) {
        User u = new User(username, password, tags);
        tableUsers.put(username, u);

        GraphNode<String> node = new GraphNode<>(username);

        String TAGS_LABEL = "TAGS";
        GroupNode tagsGroup = new GroupNode(TAGS_LABEL, node);

        String POSTS_LABEL = "POSTS";
        GroupNode postsGroup = new GroupNode(POSTS_LABEL, node);

        graph.putEdge(node, tagsGroup);
        graph.putEdge(node, postsGroup);

        for (String tag : tags) {
            GraphNode<String> tagNode = new GraphNode<>(tag);
            graph.putEdge(tagsGroup, tagNode);
        }

        u.setPostsGroupNode(postsGroup);
        u.setTagsGroupNode(tagsGroup);
        graphSaver.saveUser(username);
//        graph.saveUser(username);
    }

    public User getUser(String username) {
        return tableUsers.get(username);
    }

    public boolean checkIfUserExist(String username, String password) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return u.getPassword().equals(password);
        } catch (NullPointerException e) {
            return false;
        }
    }

    public Set<Node> discoverFriendsByTag(String username) {
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

    public Set<String> getFollowersOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));

            return Collections.unmodifiableSet(u.getFollowers());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Set<String> getFollowingOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return Collections.unmodifiableSet(u.getFollowing());
        } catch (NullPointerException e) {
            return null;
        }
    }

    //u1 segue u2
    public void followUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            user1.addFollow(u2);
            user2.addFollowers(u1);
        } catch (NullPointerException ignored) {
        }
    }

    public void unfollowUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            user1.removeFollow(u2);
            user2.removeFollowers(u1);
        } catch (NullPointerException ignored) {
        }
    }

    public UUID createPost(String author, String title, String content) {
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

            graphSaver.savePost(p);
//            graph.savePost(p);
            return p.getId();
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Post getPost(UUID idPost) {
        return tablePosts.get(idPost);
    }

    public HashMap<String, Object> viewFriendPost(String whoWantToView, UUID idPost) {
        Post p = getPost(idPost);
        User u = getUser(whoWantToView);
        if (p != null && u != null) {
            if (!u.getFollowing().contains(p.getAuthor()) && !whoWantToView.equals(p.getAuthor())) return null;

            HashMap<String, Object> result = new HashMap<>();

            GroupNode comments = p.getCommentsGroupNode();
            GroupNode likes = p.getLikesGroupNode();

            result.put("POST", p);
            result.put("COMMENTS", graph.adjacentNodes(comments));
            result.put("LIKES", graph.adjacentNodes(likes));

            return result;
        }

        return null;
    }

    public Set<Node> getAllPostsOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return graph.adjacentNodes(u.getPostsGroupNode());
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Set<Node> getFriendsPostsOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            HashSet<Node> result = new HashSet<>();

            for (String follow : u.getFollowing()) {
                User friend = tableUsers.get(follow);
                result.addAll(graph.adjacentNodes(friend.getPostsGroupNode()));
            }

            return Collections.unmodifiableSet(result);
        } catch (NullPointerException e) {
            return null;
        }
    }

    public List<Node> getLatestFriendsPostsOf(String username, Date date) {
        User u;
        try {
            u = Objects.requireNonNull(tableUsers.get(username));
        } catch (NullPointerException e) {
            return null;
        }

        TreeSet<Node> result = new TreeSet<>((o1, o2) -> {
            if (o1 instanceof GraphNode<?> g1 && o2 instanceof GraphNode<?> g2) {
                if (g1.getValue() instanceof UUID id1 && g2.getValue() instanceof UUID id2) {
                    Post p1 = tablePosts.get(id1);
                    Post p2 = tablePosts.get(id2);

                    if (p1 != null && p2 != null) {
                        return -p1.compareTo(p2);
                    }
                } else if (g1.getValue() instanceof String s1 || g2.getValue() instanceof String s2) {
                    return -1;
                }
            }
            throw new IllegalArgumentException("Tentato paragone tra 2 nodi diversi del grafo");
        });

        for (String follow : u.getFollowing()) {
            try {
                User friend = Objects.requireNonNull(tableUsers.get(follow));
                if (friend.getDateOfLastPost().after(date)) {
                    Set<Node> nodes = graph.adjacentNodes(friend.getPostsGroupNode());
                    result.addAll(nodes);
                }
            } catch (NullPointerException ignored) {
            }
        }

        LinkedList<Node> latest = new LinkedList<>();

        for (Node n : result) {
            if (n instanceof GraphNode<?> g) {
                if (g.getValue() instanceof String) continue;

                UUID id = (UUID) g.getValue();
                try {
                    Post p = Objects.requireNonNull(tablePosts.get(id));
                    Date d = p.date().toDate();
                    if (d.after(date)) {
                        latest.add(n);
                    } else {
                        break;
                    }
                } catch (NullPointerException ignored) {

                }
            }
        }

        return Collections.unmodifiableList(latest);
    }

    public void rewinFriendsPost(String username, UUID idPost) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            Post p = Objects.requireNonNull(tablePosts.get(idPost));
            if (!u.getFollowing().contains(p.getAuthor())) return;

            GroupNode posts = u.getPostsGroupNode();
            try {
                if (graph.adjacentNodes(posts).contains(new GraphNode<>(idPost))) return;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return;
            }

            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);
            graph.putEdge(posts, rewinNode);
            graphSaver.saveRewin(u, idPost);
//            graph.saveRewin(u, idPost);
        } catch (NullPointerException ignored) {

        }
    }

    public void removeRewin(String username, UUID idPost){
            User u = tableUsers.get(username);
            Post p = tablePosts.get(idPost);
            if(u != null & p != null){
                GroupNode posts = u.getPostsGroupNode();
                GraphNode<UUID> rewinNode = new GraphNode<>(idPost);

                graph.removeEdge(posts, rewinNode);
                graphSaver.removeRewinFromFile(username, idPost);
            }
    }

    public void appendComment(UUID idPost, String author, String content) {
        if (tablePosts.containsKey(idPost)) {
            Comment c = new Comment(idPost, author, content);

            GraphNode<Comment> commentNode = new GraphNode<>(c);
            GroupNode commentsGroup = tablePosts.get(idPost).getCommentsGroupNode();

            graph.putEdge(commentsGroup, commentNode);
            graph.putEdge(newEntryGroup, commentNode);
            graphSaver.saveComment(tablePosts.get(idPost).getAuthor(), c);
//            graph.saveComment(tablePosts.get(idPost).getAuthor(), c);
        }

    }

    public void appendLike(UUID idPost) {
        appendLikeDislike(idPost, Like.type.LIKE);
    }

    public void appendDislike(UUID idPost) {
        appendLikeDislike(idPost, Like.type.DISLIKE);
    }

    private void appendLikeDislike(UUID idPost, Like.type type) {
        if (tablePosts.containsKey(idPost)) {
            Like like = new Like(idPost, type);
            GraphNode<Like> likeNode = new GraphNode<>(like);
            Post post = tablePosts.get(idPost);
            if (post == null) return;

            GroupNode likesGroup = post.getLikesGroupNode();

            graph.putEdge(likesGroup, likeNode);
            graph.putEdge(newEntryGroup, likeNode);
            graphSaver.saveLike(post.getAuthor(), like);
//            graph.saveLike(tablePosts.get(idPost).getAuthor(), like);
        }

    }

    //username vuole cancellare un post con id=idPost
    public void removePost(String username, UUID idPost) {
        Post p = tablePosts.remove(idPost);
        if (p != null) {
            if(!username.equals(p.getAuthor())) return;

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

//            graph.removePost(p, commentsSet, likesSet);
            graphSaver.removePost(p, commentsSet, likesSet);
            graph.removeNode(commentsGroup);
            graph.removeNode(likesGroup);
            graph.removeNode(new GraphNode<>(p.getId()));
            savePosts();
        }
    }

    public Set<Node> pullNewEntries() {
        Set<Node> newEntries = graph.adjacentNodes(newEntryGroup);

        for (Node n : newEntries) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                graphSaver.removeEntry(tablePosts.get(c.getIdPost()).getAuthor(), c);
//                graph.removeEntry(tablePosts.get(c.getIdPost()).getAuthor(), c);
            } else if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l);
//                graph.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l);
            }
        }

        graph.clearEdges(newEntryGroup);
        return newEntries;
    }

    public void save() {
        saveUsers();
        savePosts();
    }

    private void saveUsers() {
        try {
            String s = gson.toJson(tableUsers);
            BufferedWriter out = new BufferedWriter(new PrintWriter("users.json"));
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePosts() {
        try {
            String s = gson.toJson(tablePosts);
            BufferedWriter out = new BufferedWriter(new PrintWriter("posts.json"));
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTables() throws IOException {
        File uFile = new File("users.json");
        File pFile = new File("posts.json");
        if (!uFile.exists() || !pFile.exists()) return;

        JsonReader usersReader = new JsonReader(new FileReader("users.json"));
        JsonReader postsReader = new JsonReader(new FileReader("posts.json"));
        Type tableUsersType = new TypeToken<ConcurrentHashMap<String, User>>() {
        }.getType();
        Type tablePostsType = new TypeToken<ConcurrentHashMap<UUID, Post>>() {
        }.getType();

        tableUsers = gson.fromJson(usersReader, tableUsersType);
        tablePosts = gson.fromJson(postsReader, tablePostsType);

        usersReader.close();
        postsReader.close();
    }

    public void printGraph() {
        for (Node n : graph.nodes()) {
            System.out.print(n + " -> ");
            System.out.println(graph.adjacentNodes(n));
        }
    }

    public WinsomeGraph getGraph() {
        return graph;
    }
}
