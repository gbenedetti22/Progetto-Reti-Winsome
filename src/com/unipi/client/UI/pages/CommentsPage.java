package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.CommentBanner;
import com.unipi.client.UI.components.BlueButton;
import com.unipi.client.UI.components.TextInputField;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;

public class CommentsPage extends JFrame {
    private final HomePage.BannerLayout rootPanel;
    private TextInputField inputField;
    private PostPage parent;

    public CommentsPage(PostPage parent) {
        this.parent = parent;

        setTitle("Commenti");
        JScrollPane scrollPane = HomePage.createScrollableBannerLayout(10);
        rootPanel = (HomePage.BannerLayout) scrollPane.getViewport().getView();

        inputField = new TextInputField("Inserisci commento..");
        BlueButton button = new BlueButton("Pubblica");
        button.setOnClick(()-> {
            ActionPipe.performAction(ACTIONS.PUBLISH_COMMENT_ACTION, this);
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.add(inputField);
        inputPanel.add(button);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void addComment(CommentBanner banner) {
        rootPanel.placeComponent(banner);
    }

    public String getInputText() {
        return inputField.getText();
    }

    public PostPage getPost() {
        return parent;
    }

    public void open() {
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void clearField() {
        inputField.clear();
    }
}
