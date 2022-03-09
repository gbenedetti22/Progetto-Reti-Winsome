package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedHashSet;

public class FollowersPage extends JPanel {
    public enum Type {
        FOLLOWER,
        FOLLOWING
    }

    private final Page left;
    private final Page right;
    private LinkedHashSet<String> leftComponents;
    private LinkedHashSet<String> rightComponents;

    public FollowersPage() {
        super(new BorderLayout());
        left = new Page(Type.FOLLOWER);
        right = new Page(Type.FOLLOWING);
        this.leftComponents = new LinkedHashSet<>();
        this.rightComponents = new LinkedHashSet<>();

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
            if (leftComponents.contains(name)) return;

            left.addFollowerBanner(name);
            leftComponents.add(name);
        } else {
            if (rightComponents.contains(name)) return;

            right.addFollowerBanner(name);
            rightComponents.add(name);
        }
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
        }

        public void addFollowerBanner(String name) {
            rootPanel.remove(magicComponent);

            PageBanner pageBanner = new PageBanner(name, currentType);
            pageBanner.setPreferredSize(new Dimension(100, 65));

            rootPanel.add(pageBanner);
            rootPanel.add(magicComponent);
        }

        private void addFollowerBanner(PageBanner banner) {
            rootPanel.remove(magicComponent);
            rootPanel.add(banner);
            rootPanel.add(magicComponent);
        }
    }

    private class PageBanner extends UserBanner implements MouseListener {
        private Type currentType;
        private LinkLabel actionLabel;
        private MouseListener currentMouseAction;

        public PageBanner(String username, Type type) {
            super(username);

            this.currentType = type;
            actionLabel = getActionLabel();

            if (type == Type.FOLLOWER)
                setAsFollower();
            else
                setAsFollowing();

            actionLabel.addMouseListener(new PageBanner());
        }

        public PageBanner() {
            super();
        }

        public Type userType() {
            return currentType;
        }

        public void setAsFollowing() {
            actionLabel.setText("<html><u>Unfollow</u></html>");
            if (currentMouseAction != null)
                actionLabel.removeMouseListener(currentMouseAction);

            currentMouseAction = actionLabel.addOnMouseClick(() -> ActionPipe.performAction(ACTIONS.UNFOLLOW_ACTION, getUsername()));
            currentType = Type.FOLLOWING;
        }

        public void setAsFollower() {
            actionLabel.setText("<html><u>Follow</u></html>");
            if (currentMouseAction != null)
                actionLabel.removeMouseListener(currentMouseAction);

            currentMouseAction = actionLabel.addOnMouseClick(() -> ActionPipe.performAction(ACTIONS.FOLLOW_ACTION, getUsername()));
            currentType = Type.FOLLOWER;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JLabel label = (JLabel) e.getSource();
            PageBanner banner = (PageBanner) label.getParent();

            if (banner.userType() == Type.FOLLOWER) {
                banner.setAsFollowing();

                leftComponents.remove(banner.getUsername());
                left.removeBanner(banner);
                left.revalidate();
                left.repaint();

                rightComponents.add(banner.getUsername());
                right.addFollowerBanner(banner);
            } else if (banner.userType() == Type.FOLLOWING) {
                rightComponents.remove(banner.getUsername());
                right.removeBanner(banner);
                right.revalidate();
                right.repaint();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

        }

        @Override
        public void mouseReleased(MouseEvent e) {

        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }
    }
}
