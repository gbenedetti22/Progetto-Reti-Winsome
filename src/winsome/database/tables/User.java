package winsome.database.tables;

import winsome.common.WinsomeTransaction;
import winsome.database.graph.graphNodes.GroupNode;

import java.util.*;

public class User {
    private final String username;
    private String password;
    private List<String> tags;
    private HashSet<String> following;
    private HashSet<String> followers;
    private List<WinsomeTransaction> transactions;
    private transient GroupNode tagsGroupNode;
    private transient GroupNode postsGroupNode;

    public User(String username, String password, List<String> tags) {
        this.username = username;
        this.password = password;
        this.tags = tags;
        this.transactions = new LinkedList<>();

        followers = new HashSet<>();
        following = new HashSet<>();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public boolean addFollow(String username) {
        return following.add(username);
    }

    public boolean addFollowers(String username) {
        return followers.add(username);
    }

    public boolean removeFollow(String username) {
        return following.remove(username);
    }

    public boolean removeFollowers(String username) {
        return followers.remove(username);
    }

    public Set<String> getFollowing() {
        return following;
    }

    public Set<String> getFollowers() {
        return followers;
    }

    public GroupNode getTagsGroupNode() {
        return tagsGroupNode;
    }

    public void setTagsGroupNode(GroupNode tagsGroupNode) {
        this.tagsGroupNode = tagsGroupNode;
    }

    public GroupNode getPostsGroupNode() {
        return postsGroupNode;
    }

    public void setPostsGroupNode(GroupNode postsGroupNode) {
        this.postsGroupNode = postsGroupNode;
    }

    public void addTransaction(String coins, String date) {
        transactions.add(new WinsomeTransaction(coins, date));
    }

    public List<WinsomeTransaction> getTransactions() {
        return transactions;
    }

    @Override
    public String toString() {
        return "User {" +
                "username = '" + username + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
