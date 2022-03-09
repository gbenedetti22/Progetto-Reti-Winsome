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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

//TODO: verificare UUID con pattern matching
public class Database implements WinsomeDatabase {
    private WinsomeGraph graph;
    private GraphSaver graphSaver;
    private ConcurrentHashMap<String, User> tableUsers;
    private ConcurrentHashMap<UUID, Post> tablePosts;
    private GroupNode newEntryGroup;
    private Gson gson;

    public static class DatabaseDate {
        private final String dateS;
        private final SimpleDateFormat dateSDF;

        public DatabaseDate() {
            dateS = "dd/MM/yy - hh:mm:ss";
            dateSDF = new SimpleDateFormat("dd/MM/yy - hh:mm:ss");
        }

        public String toString() {
            return dateS;
        }

        public SimpleDateFormat toSimpleDateFormat() {
            return dateSDF;
        }
    }

    public Database() {
        gson = new GsonBuilder().setPrettyPrinting().setDateFormat(getDateFormat().toString()).create();

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

    @Override
    public boolean createUser(String username, String password, List<String> tags) {
        if (tableUsers.containsKey(username)) return false;

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

        return true;
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

            return new HashSet<>();
        } catch (NullPointerException e) {
            return new HashSet<>();
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
            return new HashSet<>();
        }
    }

    @Override
    public Set<String> getFollowersOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));

            return Collections.unmodifiableSet(u.getFollowers());
        } catch (NullPointerException e) {
            return new HashSet<>();
        }
    }


    @Override
    public Set<String> getFollowingOf(String username) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            return Collections.unmodifiableSet(u.getFollowing());
        } catch (NullPointerException e) {
            return new HashSet<>();
        }
    }

    //u1 segue u2

    @Override
    public boolean followUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            boolean b1 = user1.addFollow(u2);
            boolean b2 = user2.addFollowers(u1);
            return b1 && b2;
        } catch (NullPointerException ignored) {
        }

        return false;
    }


    @Override
    public boolean unfollowUser(String u1, String u2) {
        try {
            User user1 = Objects.requireNonNull(tableUsers.get(u1));
            User user2 = Objects.requireNonNull(tableUsers.get(u2));

            boolean b1 = user1.removeFollow(u2);
            boolean b2 = user2.removeFollowers(u1);
            return b1 && b2;
        } catch (NullPointerException ignored) {
        }

        return false;
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

            graphSaver.savePost(p);
            return p;
        } catch (NullPointerException e) {
            return null;
        }
    }

    public Post getPost(UUID idPost) {
        return tablePosts.get(idPost);
    }


    @Override
    public HashMap<String, Object> viewFriendPost(String whoWantToView, UUID idPost) {
        Post p = getPost(idPost);
        User u = getUser(whoWantToView);
        if (p != null && u != null) {
            HashMap<String, Object> post = new HashMap<>();

            GroupNode comments = p.getCommentsGroupNode();
            GroupNode likes = p.getLikesGroupNode();

            post.put("TITLE", p.getTitle());
            post.put("CONTENT", p.getContent());
            post.put("LIKES", graph.adjacentNodes(likes));
            post.put("COMMENTS", graph.adjacentNodes(comments));

            return post;
        }

        return new HashMap<>();
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
        if (me == null) return new HashMap<>();

        HashMap<String, Set<Node>> result = new HashMap<>();
        SimpleDateFormat format = new SimpleDateFormat(Database.getDateFormat().toString());

        for (Map.Entry<String, String> entry : dateMap.entrySet()) {
            String follow = entry.getKey();
            String date = entry.getValue();
            if (!me.getFollowing().contains(follow)) continue;

            try {
                User friend = Objects.requireNonNull(tableUsers.get(follow));

                if(date.equals("0")){
                    Set<Node> nodes = graph.adjacentNodes(friend.getPostsGroupNode());
                    result.put(follow, nodes);
                    continue;
                }

                Date d = format.parse(date);
                //se la amico ha postato qualcosa dopo la data d
                if (friend.getDateOfLastPost().after(d)) {
                    Set<Node> nodes = graph.adjacentNodes(friend.getPostsGroupNode());
                    Set<Node> postsAfterDate = nodes.stream()
//                            .parallel()
                            .filter(node -> {
                                if (node instanceof GraphNode<?> g && g.getValue() instanceof Post p) {
                                    return p.date().toDate().after(d);
                                }

                                return false;
                            }).collect(Collectors.toSet());

                    result.put(follow, postsAfterDate);
                }


            } catch (ParseException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    @Override
    public boolean rewinFriendsPost(String username, UUID idPost) {
        try {
            User u = Objects.requireNonNull(tableUsers.get(username));
            Post p = Objects.requireNonNull(tablePosts.get(idPost));
            if (!u.getFollowing().contains(p.getAuthor())) return false;

            GroupNode posts = u.getPostsGroupNode();
            try {
                if (graph.adjacentNodes(posts).contains(new GraphNode<>(idPost))) return false;
            } catch (NullPointerException e) {
                e.printStackTrace();
                return false;
            }

            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);
            graph.putEdge(posts, rewinNode);
            graphSaver.saveRewin(u, idPost);
            return true;
        } catch (NullPointerException ignored) {

        }

        return false;
    }


    @Override
    public boolean removeRewin(String username, UUID idPost) {
        User u = tableUsers.get(username);
        Post p = tablePosts.get(idPost);
        if (u != null & p != null) {
            if (!u.getFollowing().contains(p.getAuthor())) return false;
            GroupNode posts = u.getPostsGroupNode();
            GraphNode<UUID> rewinNode = new GraphNode<>(idPost);

            graph.removeEdge(posts, rewinNode);
            graphSaver.removeRewinFromFile(username, idPost);
            return true;
        }

        return false;
    }


    @Override
    public boolean appendComment(String username, UUID idPost, String author, String content) {
        Post p = tablePosts.get(idPost);
        User u = tableUsers.get(username);

        if (p != null && u != null) {
            if (!u.getFollowing().contains(p.getAuthor())) return false;

            Comment c = new Comment(idPost, author, content);

            GraphNode<Comment> commentNode = new GraphNode<>(c);
            GroupNode commentsGroup = tablePosts.get(idPost).getCommentsGroupNode();

            graph.putEdge(commentsGroup, commentNode);
            graph.putEdge(newEntryGroup, commentNode);
            graphSaver.saveComment(tablePosts.get(idPost).getAuthor(), c);
            return true;
        }

        return false;
    }


    @Override
    public boolean appendLike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.type.LIKE);
    }


    @Override
    public boolean appendDislike(String username, UUID idPost) {
        return appendLikeDislike(username, idPost, Like.type.DISLIKE);
    }

    private boolean appendLikeDislike(String username, UUID idPost, Like.type type) {
        User u = tableUsers.get(username);
        Post p = tablePosts.get(idPost);
        if (u == null || p == null) return false;
        if (!u.getFollowing().contains(p.getAuthor())) return false;
        if(u.getUsername().equals(p.getAuthor())) return false;

        Post post = tablePosts.get(idPost);
        if (post == null) return false;

        GroupNode likesGroup = post.getLikesGroupNode();

        Set<Node> likes = graph.adjacentNodes(post.getLikesGroupNode());
//        for (Node n : likes) {
//            if(n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
//                if(l.getUsername().equals(username) && l.getType() != type) {
//                    l.setType(type);
//                    graph.putEdge(newEntryGroup, n);
//                    graphSaver.saveLike(username, l);
//                }
//                else if(l.getUsername().equals(username) && l.getType() == type)
//                    return false;
//            }
//        }
        for (Node n : likes) {
            if(n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                if(l.getUsername().equals(username))
                    return false;
            }
        }

        Like like = new Like(idPost, type, username);
        GraphNode<Like> likeNode = new GraphNode<>(like);

        graph.putEdge(likesGroup, likeNode);
        graph.putEdge(newEntryGroup, likeNode);
        graphSaver.saveLike(post.getAuthor(), like);
        return true;
    }

    //username vuole cancellare un post con id=idPost
    @Override
    public boolean removePost(String username, UUID idPost) {
        Post p = tablePosts.remove(idPost);
        if (p != null) {
            if (!username.equals(p.getAuthor())) return false;

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

            graphSaver.removePost(p, commentsSet, likesSet);
            graph.removeNode(commentsGroup);
            graph.removeNode(likesGroup);
            graph.removeNode(new GraphNode<>(p.getId()));
            savePosts();
            return true;
        }

        return false;
    }


    @Override
    public Set<Node> pullNewEntries() {
        Set<Node> newEntries = graph.adjacentNodes(newEntryGroup);

        for (Node n : newEntries) {
            if (n instanceof GraphNode<?> g && g.getValue() instanceof Comment c) {
                graphSaver.removeEntry(tablePosts.get(c.getIdPost()).getAuthor(), c);
            } else if (n instanceof GraphNode<?> g && g.getValue() instanceof Like l) {
                graphSaver.removeEntry(tablePosts.get(l.getIdPost()).getAuthor(), l);
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

    public void printGraph() {
        for (Node n : graph.nodes()) {
            System.out.print(n + " -> ");
            System.out.println(graph.adjacentNodes(n));
        }
    }

    public WinsomeGraph getGraph() {
        return graph;
    }

    public static DatabaseDate getDateFormat() {
        return new DatabaseDate();
    }

    public static String getName() {
        return "graphDB";
    }
}
