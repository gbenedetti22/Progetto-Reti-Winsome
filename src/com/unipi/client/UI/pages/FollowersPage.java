package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Objects;

public class FollowersPage extends JPanel {
    private final Page left;    //followers
    private final Page right;   //following
    private HashMap<String, PageBanner> leftComponents;
    private HashMap<String, PageBanner> rightComponents;

    public FollowersPage() {
        super(new BorderLayout());
        left = new Page(Type.FOLLOWER);
        right = new Page(Type.FOLLOWING);
        this.leftComponents = new HashMap<>();
        this.rightComponents = new HashMap<>();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(left, JSplitPane.LEFT);
        splitPane.add(right, JSplitPane.RIGHT);
        splitPane.setDividerSize(0);
        splitPane.setResizeWeight(.43);

        LinkLabel backLabel = new LinkLabel("Indietro");
        backLabel.setTextSize(22);
        backLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.BACKPAGE_ACTION));

        add(splitPane);
        add(backLabel, BorderLayout.NORTH);
    }

    public void appendBanner(String name, Type type) {
        if (type == Type.FOLLOWER) {
            if (leftComponents.containsKey(name)) return;

            PageBanner b = left.addFollowerBanner(name);
            leftComponents.put(name, b);
        } else {
            if (rightComponents.containsKey(name)) return;

            PageBanner b = right.addFollowerBanner(name);
            rightComponents.put(name, b);
        }
    }

    public void addRight(PageBanner banner) {
        if (rightComponents.containsKey(banner.getUsername())) return;

        right.addFollowerBanner(banner);
        rightComponents.put(banner.getUsername(), banner);
    }

    public void addLeft(PageBanner banner) {
        if (leftComponents.containsKey(banner.getUsername())) return;

        left.addFollowerBanner(banner);
        leftComponents.put(banner.getUsername(), banner);
    }

    public void removeFromLeft(PageBanner banner) {
        if (!leftComponents.containsKey(banner.getUsername())) return;

        left.removeBanner(banner);
        leftComponents.remove(banner.getUsername());
    }

    public void removeFromRight(PageBanner banner) {
        if (!rightComponents.containsKey(banner.getUsername())) return;

        right.removeBanner(banner);
        rightComponents.remove(banner.getUsername());
    }

    public void removeFromLeft(String username) {
        if (!leftComponents.containsKey(username)) return;

        left.removeBanner(leftComponents.get(username));
        leftComponents.remove(username);
    }

    public void removeFromRight(String username) {
        if (!rightComponents.containsKey(username)) return;

        right.removeBanner(rightComponents.get(username));
        rightComponents.remove(username);
    }

    public PageBanner getFromLeft(String username) {
        return leftComponents.get(username);
    }

    public PageBanner getFromRight(String username) {
        return rightComponents.get(username);
    }

    public PageBanner newBanner(String username, Type type) {
        return new PageBanner(username, type);
    }

    public void clear() {
        left.clear();
        right.clear();
        leftComponents.clear();
        rightComponents.clear();
    }

    public void clearLeft() {
        left.clear();
        leftComponents.clear();
    }

    public void clearRight() {
        right.clear();
        rightComponents.clear();
    }

    public enum Type {
        FOLLOWER,
        FOLLOWING
    }

    private class Page extends JPanel {
        private final JPanel rootPanel;
        private final Type currentType;
        private final Component magicComponent = Box.createVerticalGlue();

        public Page(Type type) {
            super(new BorderLayout());

            this.currentType = type;

            JLabel followersLabel = new JLabel("", SwingConstants.CENTER);
            followersLabel.setFont(new Font("Arial", Font.PLAIN, 22));
            followersLabel.setOpaque(true);
            followersLabel.setBackground(Color.WHITE);
            if (type == Type.FOLLOWER)
                followersLabel.setText("Followers");
            else
                followersLabel.setText("Seguiti");

            rootPanel = new JPanel();
            rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
            rootPanel.add(magicComponent);
            JScrollPane scrollPane = new JScrollPane(rootPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            add(followersLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void removeBanner(PageBanner comp) {
            rootPanel.remove(comp);
            rootPanel.revalidate();
            rootPanel.repaint();
        }

        public PageBanner addFollowerBanner(String name) {
            rootPanel.remove(magicComponent);

            PageBanner banner = new PageBanner(name, currentType);
            banner.setPreferredSize(new Dimension(100, 65));

            rootPanel.add(banner);
            rootPanel.add(magicComponent);
            return banner;
        }

        private void addFollowerBanner(PageBanner banner) {
            rootPanel.remove(magicComponent);

            banner.setPreferredSize(new Dimension(100, 65));
            rootPanel.add(banner);
            rootPanel.add(magicComponent);
        }

        public void clear() {
            rootPanel.removeAll();
        }
    }

    public class PageBanner extends UserBanner {
        private String username;
        private Type currentType;
        private LinkLabel actionLabel;

        public PageBanner(String username, Type type) {
            super(username);

            this.currentType = type;
            this.username = username;
            actionLabel = getActionLabel();

            if (type == Type.FOLLOWER)
                doFollow();
            else
                doUnfollow();
        }

        public PageBanner() {
            super();
        }

        public Type userType() {
            return currentType;
        }

        @Override
        public String getUsername() {
            return username;
        }

        public void doUnfollow() {
            actionLabel.setText("<html><u>Unfollow</u></html>");

            actionLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.UNFOLLOW_ACTION, getUsername()));
            currentType = Type.FOLLOWING;
        }

        public void doFollow() {
            actionLabel.setText("<html><u>Follow</u></html>");

            actionLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.FOLLOW_ACTION, getUsername()));
            currentType = Type.FOLLOWER;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            PageBanner banner = (PageBanner) o;
            return username.equals(banner.username);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), username);
        }
    }
}
