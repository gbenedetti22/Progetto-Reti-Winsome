package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.CommentBanner;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PostPage extends JPanel {
    private final JTextPane textPane;
    private final String title;
    private int numLike;
    private int numDislike;
    private boolean likeSetted;
    private boolean dislikeSetted;
    private JLabel labelLike;
    private JLabel labelDislike;
    private CommentsPage commentsPage;
    private String id;
    private String author;

    public PostPage(String id, String author, String title, String content) {
        super(new BorderLayout());
        this.title = title;
        this.author = author;
        this.id = id;
        this.commentsPage = new CommentsPage(this);
        this.likeSetted = false;
        this.dislikeSetted = false;

        textPane = new JTextPane();
        textPane.setEditorKit(new WrapEditorKit());
        textPane.setMinimumSize(new Dimension());

        setTitle(title);
        setContent(content);

        textPane.setEditable(false);

        JPanel upperPanel = initUpperPanel();

        add(upperPanel, BorderLayout.CENTER);

        LinkLabel backLabel = new LinkLabel("Indietro");
        backLabel.setBorder(BorderFactory.createEmptyBorder(10, 7, 10, 0));
        backLabel.setBackground(Color.WHITE);
        backLabel.setOpaque(true);
        backLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.BACKPAGE_ACTION));

        add(backLabel, BorderLayout.NORTH);
    }

    private void setContent(String content) {
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet left = new SimpleAttributeSet();
        StyleConstants.setAlignment(left, StyleConstants.ALIGN_LEFT);

        StyleContext sc = StyleContext.getDefaultStyleContext();
        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Arial");
        aset = sc.addAttribute(aset, StyleConstants.FontSize, 17);

        doc.setParagraphAttributes(doc.getLength(), 1, left, false);
        try {
            doc.insertString(doc.getLength(), content, aset);
        } catch (Exception ignored) {
        }
    }

    private JPanel initUpperPanel() {
        JPanel postTopPanel = new JPanel(new BorderLayout());

        JPanel postContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.ipadx = (1280 * 60) / 100;
        gbc.ipady = (720 * 60) / 100;
        postContainer.add(textPane, gbc);

        JPanel likeDislikePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        likeDislikePanel.setBackground(Color.WHITE);

        JLabel like = new JLabel("●");
        like.setFont(new Font("Arial", Font.BOLD, 45));
        like.setForeground(Color.GREEN);
        like.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        like.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ActionPipe.performAction(ACTIONS.LIKE_ACTION);
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
        });

        labelLike = new JLabel("0");
        labelLike.setFont(new Font("Arial", Font.PLAIN, 15));
        labelLike.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        JLabel dislike = new JLabel("●");
        dislike.setFont(new Font("Arial", Font.BOLD, 45));
        dislike.setForeground(Color.RED);
        dislike.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dislike.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ActionPipe.performAction(ACTIONS.DISLIKE_ACTION);
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
        });

        labelDislike = new JLabel("0");
        labelDislike.setFont(new Font("Arial", Font.PLAIN, 15));

        JLabel authorLabel = new JLabel("Autore del Post: " + author);
        authorLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        authorLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        LinkLabel comments = new LinkLabel("", SwingConstants.RIGHT);
        comments.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));
        comments.setIcon(getCommentIcon());
        comments.setOnMouseClick(()-> ActionPipe.performAction(ACTIONS.GET_LATEST_COMMENTS, commentsPage));

        likeDislikePanel.add(like);
        likeDislikePanel.add(labelLike);
        likeDislikePanel.add(dislike);
        likeDislikePanel.add(labelDislike);
        likeDislikePanel.add(authorLabel);

        JPanel instrumentsPanel = new JPanel(new BorderLayout());
        instrumentsPanel.setBackground(Color.WHITE);
        instrumentsPanel.add(likeDislikePanel, BorderLayout.LINE_START);
        instrumentsPanel.add(comments, BorderLayout.LINE_END);

        gbc.ipady = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;

        likeDislikePanel.setBackground(Color.WHITE);
        postContainer.add(instrumentsPanel, gbc);
        postTopPanel.add(postContainer, BorderLayout.CENTER);

        return postTopPanel;
    }

    public void addComment(CommentBanner postBanner) {
        commentsPage.addComment(postBanner);
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        if (!title.contains("\n"))
            title = title.concat("\n");

        StyleContext sc = StyleContext.getDefaultStyleContext();

        AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.FontFamily, "Arial Bold");
        aset = sc.addAttribute(aset, StyleConstants.FontSize, 22);

        textPane.setCaretPosition(0);
        textPane.setCharacterAttributes(aset, false);

        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);

        textPane.replaceSelection(title);

        StyledDocument doc = textPane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
    }

    public String getId() {
        return id;
    }

    public void setLikes(int likes) {
        labelLike.setText(String.valueOf(likes));
        numLike = likes;
    }

    public void setDislikes(int dislikes) {
        labelDislike.setText(String.valueOf(dislikes));
        numDislike = dislikes;
    }

    public void addLike() {
        if(dislikeSetted) {
            numDislike--;
            labelDislike.setText(String.valueOf(numDislike));
            dislikeSetted = false;
        }

        numLike++;
        labelLike.setText(String.valueOf(numLike));
        likeSetted = true;
    }

    public void addDislike() {
        if(likeSetted) {
            numLike--;
            labelLike.setText(String.valueOf(numLike));
            likeSetted = false;
        }

        numDislike++;
        labelDislike.setText(String.valueOf(numDislike));
        dislikeSetted = true;
    }

    public void setLikeSetted(boolean likeSetted) {
        this.likeSetted = likeSetted;
    }

    public void setDislikeSetted(boolean dislikeSetted) {
        this.dislikeSetted = dislikeSetted;
    }

    private ImageIcon getCommentIcon() {
        ImageIcon icon = new ImageIcon("./resources/commentsIcon.png");
        Image image = icon.getImage();
        Image scaledImg = image.getScaledInstance(40, 30, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImg);
    }

    private static class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private static class WrapColumnFactory implements ViewFactory {
        public View create(Element elem) {
            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                    case AbstractDocument.ContentElementName:
                        return new WrapLabelView(elem);
                    case AbstractDocument.ParagraphElementName:
                        return new ParagraphView(elem);
                    case AbstractDocument.SectionElementName:
                        return new BoxView(elem, View.Y_AXIS);
                    case StyleConstants.ComponentElementName:
                        return new ComponentView(elem);
                    case StyleConstants.IconElementName:
                        return new IconView(elem);
                }
            }
            return new LabelView(elem);
        }
    }

    private static class WrapLabelView extends LabelView {
        public WrapLabelView(Element elem) {
            super(elem);
        }

        public float getMinimumSpan(int axis) {
            return switch (axis) {
                case View.X_AXIS -> 0;
                case View.Y_AXIS -> super.getMinimumSpan(axis);
                default -> throw new IllegalArgumentException("Invalid axis: " + axis);
            };
        }
    }
}
