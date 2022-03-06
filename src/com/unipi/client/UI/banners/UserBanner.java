package com.unipi.client.UI.banners;

import com.unipi.client.UI.components.LinkLabel;

import javax.swing.*;
import java.awt.*;

public class UserBanner extends JPanel {
    private JLabel usernameLabel;
    private LinkLabel actionLabel;

    public UserBanner(String username) {
        super(new GridLayout(1, 2));

        this.usernameLabel = new JLabel(username);
        this.usernameLabel.setFont(new Font("Arial", Font.PLAIN, 22));
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        add(usernameLabel);
        actionLabel = new LinkLabel("Follow");
        actionLabel.setTextSize(22);

        setBackground(Color.WHITE);

        add(actionLabel);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        setMaximumSize(new Dimension(1920, 50));
    }

    public UserBanner() {
    }

    protected JLabel getActionLabel() {
        return actionLabel;
    }

    public void setActionLabelText(String text) {
        this.actionLabel.setText(text);
    }

    public String getUsername() {
        return usernameLabel.getText();
    }
}
