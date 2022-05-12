package winsome.client.UI.pages;

import winsome.client.UI.banners.CommentBanner;
import winsome.client.UI.components.BlueButton;
import winsome.client.UI.components.TextInputField;
import winsome.client.mainFrame.ACTIONS;
import winsome.client.mainFrame.ActionPipe;
import winsome.common.SimpleComment;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class CommentsPage extends JFrame {
    private final HomePage.BannerLayout rootPanel;
    private TextInputField inputField;
    private PostPage parent;
    private TreeSet<CommentBanner> comments;

    public CommentsPage(PostPage parent) {
        this.parent = parent;
        this.comments = new TreeSet<>(Comparator.reverseOrder());

        setTitle("Commenti");
        JScrollPane scrollPane = HomePage.createScrollableBannerLayout(10);
        rootPanel = (HomePage.BannerLayout) scrollPane.getViewport().getView();

        inputField = new TextInputField("Inserisci commento..");
        BlueButton button = new BlueButton("Pubblica");
        button.setOnClick(() -> {
            ActionPipe.performAction(ACTIONS.PUBLISH_COMMENT_ACTION, this);
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.add(inputField);
        inputPanel.add(button);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }

    public void addComment(CommentBanner banner) {
        if (comments.contains(banner)) return;

        rootPanel.placeComponent(banner);
        comments.add(banner);
    }

    public TreeSet<CommentBanner> getComments() {
        return comments;
    }

    public void addAll(Set<SimpleComment> set) {
        if (set.isEmpty()) return;

        for (SimpleComment c : set) {
            comments.add(new CommentBanner(c.getId(), c.getAuthor(), c.getContent(), c.getDate()));
        }

        rootPanel.removeAll();
        for (CommentBanner banner : comments)
            rootPanel.placeComponent(banner);
    }

    public String getInputText() {
        return inputField.getText();
    }

    public PostPage getPost() {
        return parent;
    }

    public void open() {
        clearField();
        setMinimumSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void clearField() {
        inputField.clear();
    }
}
