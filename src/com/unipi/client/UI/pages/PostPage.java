package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.components.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class PostPage extends JPanel {
    private final JTextPane textPane;
    private GridBagConstraints gbc;
    private final String title;
    private HomePage.BannerLayout gridPanel;
    private int numLike;
    private int numDislike;
    private JLabel labelLike;
    private JLabel labelDislike;

    public PostPage(String title, String content) {
        super(new BorderLayout());
        this.title = title;
        textPane = new JTextPane();
        textPane.setEditorKit(new WrapEditorKit());

        setTitle(title);
        setContent(content);

        textPane.setEditable(false);

        JPanel upperPanel = initUpperPanel();
        JScrollPane downPanel = HomePage.createScrollableBannerLayout(10);

        gridPanel = (HomePage.BannerLayout) downPanel.getViewport().getView();

        JPanel framePanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.ipady = 50;
        gbc.gridx = 0;
        gbc.gridy = 0;
        framePanel.add(upperPanel, gbc);

        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        framePanel.add(downPanel, gbc);

        JScrollPane scrollPane = new JScrollPane(framePanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        framePanel.setPreferredSize(new Dimension(1280, 720 * 2));
        scrollPane.setPreferredSize(framePanel.getPreferredSize());

        add(scrollPane, BorderLayout.CENTER);
        JLabel backLabel = new JLabel("<html><u>Indietro</u></html>", SwingConstants.LEFT);
        backLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        backLabel.setBorder(BorderFactory.createEmptyBorder(10, 7, 10, 0));
        backLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backLabel.setForeground(Color.BLUE);
        backLabel.setBackground(Color.WHITE);
        backLabel.setOpaque(true);

        add(backLabel, BorderLayout.NORTH);
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
        gbc = new GridBagConstraints();

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


        labelLike = new JLabel("0");
        labelLike.setFont(new Font("Arial", Font.PLAIN, 15));
        labelLike.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 15));

        JLabel dislike = new JLabel("●");
        dislike.setFont(new Font("Arial", Font.BOLD, 45));
        dislike.setForeground(Color.RED);
        dislike.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        labelDislike = new JLabel("0");
        labelDislike.setFont(new Font("Arial", Font.PLAIN, 15));

        likeDislikePanel.add(like);
        likeDislikePanel.add(labelLike);
        likeDislikePanel.add(dislike);
        likeDislikePanel.add(labelDislike);

        gbc.ipady = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;

        likeDislikePanel.setBackground(Color.WHITE);
        postContainer.add(likeDislikePanel, gbc);

        JTextField input = new JTextField();
        gbc.ipady = 20;

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(35, 0, 0, 0);
        postContainer.add(input, gbc);

        BlueButton button = new BlueButton("Pubblica");

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(14, 0, 0, 0);
        postContainer.add(button, gbc);
        postTopPanel.add(postContainer, BorderLayout.CENTER);

        return postTopPanel;
    }

    public void addComment(PostBanner postBanner) {
        gridPanel.placeComponent(postBanner);
    }

    public void addLike(){
        numLike++;
        labelLike.setText(String.valueOf(numLike));
    }

    public void addDislike(){
        numDislike++;
        labelDislike.setText(String.valueOf(numDislike));
    }

    public String getTitle() {
        return title;
    }

    private class WrapEditorKit extends StyledEditorKit {
        ViewFactory defaultFactory = new WrapColumnFactory();

        public ViewFactory getViewFactory() {
            return defaultFactory;
        }
    }

    private class WrapColumnFactory implements ViewFactory {
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

    private class WrapLabelView extends LabelView {
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
