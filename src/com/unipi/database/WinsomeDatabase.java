package com.unipi.database;

import com.unipi.database.graph.graphNodes.Node;
import com.unipi.database.tables.Comment;
import com.unipi.database.tables.Post;
import com.unipi.database.tables.User;
import com.unipi.database.utility.EntriesStorage;

import java.util.*;

public interface WinsomeDatabase {
    String createUser(String username, String password, List<String> tags);

    User getUser(String username);

    Set<Node> getTagsIf(String username, String password);

    Set<Node> getUsersByTag(String username);

    Set<String> getFollowersOf(String username);

    Set<String> getFollowingOf(String username);

    String followUser(String u1, String u2);

    String unfollowUser(String u1, String u2);

    Post createPost(String author, String title, String content);

    HashMap<String, Object> viewFriendPost(String author, UUID idPost);
    HashMap<String, Object> openRewin(String author, UUID idPost);

    Set<Node> getAllPostsOf(String username);

    Map<String, Set<Node>> getFriendsPostsOf(String username);

    Map<String, Set<Node>> getLatestFriendsPostsOf(String username, Map<String, String> dateMap);

    String rewinFriendsPost(String username, UUID idPost);

    String removeRewin(String username, UUID idPost);

    Comment appendComment(UUID idPost, String author, String content);

    String appendLike(String username, UUID idPost);

    String appendDislike(String username, UUID idPost);

    String removePost(String username, UUID idPost);

    ArrayList<EntriesStorage.Entry> pullNewEntries();
}
