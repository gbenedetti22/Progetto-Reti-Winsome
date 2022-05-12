package winsome.client.UI.banners;

import winsome.client.UI.components.LinkLabel;
import winsome.client.mainFrame.ACTIONS;
import winsome.client.mainFrame.ActionPipe;
import winsome.common.SimplePost;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Objects;
import java.util.UUID;

public class PostBanner extends JPanel implements Comparable<PostBanner> {
    private String id;
    private String author;
    private LinkLabel retweet;
    private JTextArea placeholder;
    private JLabel info;
    private JPanel optionPanel;
    private String date;
    private SimplePost attached_post;
    private String rewin;

    protected PostBanner(String id, String author_name, String title, String content, String date, boolean deletable, boolean mouseClick) {
        this.author = author_name;
        this.date = date;
        this.id = id;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        setBackground(Color.WHITE);

        JPanel topPostBanner = new JPanel(new BorderLayout());
        optionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 19));
        headerLabel.setHorizontalTextPosition(JLabel.LEFT);

        retweet = new LinkLabel("Rewin");
        retweet.setMargin(new Insets(0, 0, 0, 7));
        retweet.setVisible(false);
        retweet.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.RETWEET_ACTION, this));

        if (deletable) {
            setDeletable();
        }

        optionPanel.add(retweet);

        topPostBanner.add(headerLabel, BorderLayout.LINE_START);
        topPostBanner.add(optionPanel, BorderLayout.LINE_END);

        placeholder = new JTextArea(content);
        placeholder.setFont(new Font("Arial", Font.PLAIN, 15));
        placeholder.setText(content);
        placeholder.setMargin(new Insets(4, 0, 0, 0));
        placeholder.setEditable(false);
        placeholder.setLineWrap(true);

        info = new JLabel(author.concat(" - " + date));
        info.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(), null));
        info.setFont(new Font("Arial", Font.PLAIN, 16));
        info.setForeground(Color.GRAY);
        info.setHorizontalTextPosition(JLabel.RIGHT);

        //TODO: se è un rewin, usare questo
//        topPostBanner.setBackground(new Color(0xC2D5D5));
        add(topPostBanner, BorderLayout.NORTH);
        add(placeholder, BorderLayout.CENTER);
        add(info, BorderLayout.PAGE_END);

        if (mouseClick)
            setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.VIEW_POST_ACTION, this));
    }

    public PostBanner(SimplePost p) {
        this(p.getId(), p.getAuthor(), p.getTitle(), p.getContent(), p.getDate(), false, true);
        this.attached_post = p;
    }

    public PostBanner(SimplePost p, boolean deletable) {
        this(p.getId(), p.getAuthor(), p.getTitle(), p.getContent(), p.getDate(), deletable, true);
        this.attached_post = p;
    }

    public void setDeletable() {
        LinkLabel deletePost = new LinkLabel("Elimina");
        deletePost.setOnMouseClick(() -> {
            int response = JOptionPane.showConfirmDialog(null, "Sei sicuro di voler cancellare questo Post?");
            if (response == JOptionPane.YES_OPTION)
                ActionPipe.performAction(ACTIONS.DELETE_POST_ACTION, this);
        });
        optionPanel.add(deletePost);
    }

    public String getAuthor() {
        return author;
    }

    public String getID() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getRewin() {
        return rewin;
    }

    public SimplePost getAttachedPost() {
        return attached_post;
    }

    public void setAsRewin(String rewinnedBy) {
        String text = String.format("Rewin di -> %s - %s", rewinnedBy, date);
        info.setText(text);
        this.rewin = rewinnedBy;
    }

    public boolean isRewin() {
        return rewin != null;
    }

    public void setRewinnable(boolean value) {
        retweet.setVisible(value);
        if (value) {
            rewin = null;
            info.setText(author.concat(" - " + date));
        }
    }

    public void setOnMouseClick(Runnable runnable) {
        if (runnable == null)
            return;

        MouseListener listener = new MouseListener() {
            private final Border previousBorder = getBorder();

            @Override
            public void mouseClicked(MouseEvent e) {
                setBorder(previousBorder);
                runnable.run();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorder(previousBorder);
            }
        };
        placeholder.addMouseListener(listener);
        addMouseListener(listener);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public int hashCode() {
        return Objects.hash(UUID.fromString(id), (rewin == null ? 0 : rewin.hashCode()));
    }

    @Override
    public String toString() {
        return "" + id + ": " + author + " -> " + this.placeholder.getText();
    }

    @Override
    public int compareTo(PostBanner o) {
        if (this == o) return 0;
        if (this.hashCode() == o.hashCode()) return 0;

        return o.attached_post.compareTo(attached_post);
    }
}
