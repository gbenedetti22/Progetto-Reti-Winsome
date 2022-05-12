package winsome.client.UI.banners;

import winsome.client.UI.components.LinkLabel;
import winsome.client.mainFrame.ACTIONS;
import winsome.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class UserBanner extends JPanel {
    private JLabel usernameLabel;
    private LinkLabel actionLabel;
    private ACTIONS currentAction;

    public UserBanner(String username) {
        super(new GridLayout(1, 2));

        this.currentAction = ACTIONS.FOLLOW_ACTION;
        this.usernameLabel = new JLabel(username);
        this.usernameLabel.setFont(new Font("Arial", Font.PLAIN, 22));
        usernameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        add(usernameLabel);
        actionLabel = new LinkLabel("Follow");
        actionLabel.setOnMouseClick(() -> ActionPipe.performAction(currentAction, this));
        actionLabel.setTextSize(22);

        setBackground(Color.WHITE);

        add(actionLabel);
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        setMaximumSize(new Dimension(1920, 50));
    }

    public UserBanner() {
    }

    protected LinkLabel getActionLabel() {
        return actionLabel;
    }

    public void setFollow() {
        currentAction = ACTIONS.FOLLOW_ACTION;
        actionLabel.setText("Follow");
    }

    public void setUnfollow() {
        currentAction = ACTIONS.UNFOLLOW_ACTION;
        actionLabel.setText("Unfollow");
    }

    public String getUsername() {
        return usernameLabel.getText();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserBanner that = (UserBanner) o;
        return usernameLabel.getText().equals(that.usernameLabel.getText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(usernameLabel.getText());
    }
}
