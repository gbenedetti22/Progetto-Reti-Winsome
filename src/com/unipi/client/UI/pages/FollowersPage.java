package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.UserBanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class FollowersPage extends JPanel {
    public enum Type{
        FOLLOWER,
        FOLLOWING
    }
    private final Page left;
    private final Page right;

    public FollowersPage(){
        super(new BorderLayout());
        left = new Page(Type.FOLLOWER);
        right = new Page(Type.FOLLOWING);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(left, JSplitPane.LEFT);
        splitPane.add(right, JSplitPane.RIGHT);
        splitPane.setDividerSize(0);
        splitPane.setResizeWeight(.43);

        JLabel backLabel = new JLabel("<html><u>Indietro</u></html>", SwingConstants.LEFT);
        backLabel.setFont(new Font("Arial", Font.PLAIN, 22));
        backLabel.setForeground(Color.BLUE);
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 0));

        add(splitPane);
        add(backLabel, BorderLayout.NORTH);
    }

    public void appendBanner(String name, Type type){
        if(type == Type.FOLLOWER)
            left.addFollowerBanner(name);
        else
            right.addFollowerBanner(name);
    }

    private class Page extends JPanel {
        private final JPanel rootPanel;
        private final Type currentType;

        public Page(Type type){
            super(new BorderLayout());

            this.currentType = type;

            JLabel followersLabel = new JLabel("", SwingConstants.CENTER);
            followersLabel.setFont(new Font("Arial", Font.PLAIN, 22));
            followersLabel.setOpaque(true);
            followersLabel.setBackground(Color.WHITE);
            if(type == Type.FOLLOWER)
                followersLabel.setText("Followers");
            else
                followersLabel.setText("Seguiti");

            rootPanel = new JPanel();
            rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
            JScrollPane scrollPane = new JScrollPane(rootPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(15);

            add(followersLabel, BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void removeBanner(JPanel comp){
            rootPanel.remove(comp);
        }

        public void addFollowerBanner(String name) {
            PageBanner pageBanner = new PageBanner(name, currentType);

            rootPanel.add(pageBanner);
        }
    }

    private class PageBanner extends UserBanner implements MouseListener {
        private Type currentType;
        private JLabel actionLabel;

        public PageBanner(String username, Type type) {
            super(username);

            this.currentType = type;
            actionLabel = getActionLabel();

            if(type == Type.FOLLOWER)
                setAsNewUser();
            else
                setAsFollower();

            actionLabel.addMouseListener(new PageBanner());
        }

        public PageBanner() {
            super();
        }

        public Type userType() {
            return currentType;
        }

        public void setAsFollower(){
            actionLabel.setText("<html><u>Unfollow</u></html>");
            currentType = Type.FOLLOWING;
        }

        public void setAsNewUser(){
            actionLabel.setText("<html><u>Follow</u></html>");
            currentType = Type.FOLLOWER;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            JLabel label = (JLabel) e.getSource();
            PageBanner banner = (PageBanner) label.getParent();

            //decido di seguire un utente
            if(banner.userType() == Type.FOLLOWER){
                banner.setAsFollower();

                left.removeBanner(banner);
                left.revalidate();
                left.repaint();

                right.rootPanel.add(banner);
                right.revalidate();
                right.repaint();
            }else if (banner.userType() == Type.FOLLOWING){
                banner.setAsNewUser();

                right.removeBanner(banner);
                right.revalidate();
                right.repaint();

                left.rootPanel.add(banner);
                left.revalidate();
                left.repaint();
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
