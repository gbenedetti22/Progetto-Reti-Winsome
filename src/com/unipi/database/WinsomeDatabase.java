package com.unipi.database;

import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;

import java.util.*;

public interface WinsomeDatabase {
    boolean createUser(String username, String password, List<String> tags);

    User getUser(String username);

    Set<Node> getTagsIf(String username, String password);

    Set<Node> getUsersByTag(String username);

    Set<String> getFollowersOf(String username);

    Set<String> getFollowingOf(String username);

    boolean followUser(String u1, String u2);

    boolean unfollowUser(String u1, String u2);

    Post createPost(String author, String title, String content);

    HashMap<String, Object> viewFriendPost(String whoWantToView, UUID idPost);

    Set<Node> getAllPostsOf(String username);

    Map<String, Set<Node>> getFriendsPostsOf(String username);

    Map<String, Set<Node>> getLatestFriendsPostsOf(String username, Date date);

    boolean rewinFriendsPost(String username, UUID idPost);

    boolean removeRewin(String username, UUID idPost);

    boolean appendComment(String username, UUID idPost, String author, String content);

    boolean appendLike(String username, UUID idPost);

    boolean appendDislike(String username, UUID idPost);

    boolean removePost(String username, UUID idPost);

    Set<Node> pullNewEntries();
}
