package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public class HomePage extends JPanel {
    private final BannerLayout gridPanel;
    private JLabel tagLabel;
    private TreeSet<PostBanner> banners;

    public HomePage(ArrayList<String> tags) {
        super(new BorderLayout(), true);
        this.tagLabel = new JLabel();
        this.banners = new TreeSet<>();

        JScrollPane scrollPane = createScrollableBannerLayout(15);
        gridPanel = (BannerLayout) scrollPane.getViewport().getView();
        gridPanel.showBackground();

        JPanel topBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBarPanel.setPreferredSize(new Dimension(35, 35));
        topBarPanel.setBackground(Color.WHITE);
        topBarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Color.LIGHT_GRAY));

        LinkLabel profileLabel = new LinkLabel("Il mio Account");
        profileLabel.setTextSize(16);
        profileLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.PROFILE_ACTION, null));

        LinkLabel updateLabel = new LinkLabel("Aggiorna");
        updateLabel.setTextSize(16);
        updateLabel.setMargin(new Insets(0, 0, 0, 17));
        updateLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.UPDATE_HOME, null));

        topBarPanel.add(updateLabel);
        topBarPanel.add(profileLabel);

        if (!tags.isEmpty()) {
            setTags(tags);
        }

        add(scrollPane, BorderLayout.CENTER);
        add(topBarPanel, BorderLayout.NORTH);
        add(tagLabel, BorderLayout.LINE_START);
    }

    public HomePage() {
        this(new ArrayList<>());
    }

    public static JScrollPane createScrollableBannerLayout(int scrollSpeed) {
        BannerLayout layout = new BannerLayout();
        JScrollPane scrollPane = new JScrollPane(layout);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(scrollSpeed);

        return scrollPane;
    }

    public void setTags(ArrayList<String> tags) {
        StringBuilder base = new StringBuilder("<html><div style='text-align: center;'>");
        for (String tag : tags) {
            base.append(tag).append("<br>");
        }
        base.append("</div></html>");
        tagLabel.setText(base.toString());
        tagLabel.setOpaque(true);
        tagLabel.setBorder(BorderFactory.createEmptyBorder(0, 50, 0, 50));
        tagLabel.setFont(new Font("Arial", Font.BOLD, 22));
        tagLabel.setBackground(Color.WHITE);
    }

    public void addPost(PostBanner postBanner) {
        if (gridPanel.isBackgroundSetted())
            gridPanel.removeBackground();

        gridPanel.placeComponent(postBanner);
        banners.add(postBanner);
    }

    public void removePost(PostBanner banner) {
        if (!banners.contains(banner)) return;

        gridPanel.remove(banner);
        gridPanel.revalidate();
        gridPanel.repaint();
        banners.remove(banner);
    }

    public void addAll(List<PostBanner> banners) {
        if (gridPanel.isBackgroundSetted())
            gridPanel.removeBackground();

        gridPanel.offset = 0;

        for (PostBanner p : this.banners) {
            gridPanel.remove(p);
        }

        this.banners.addAll(banners);

        for (PostBanner p : this.banners) {
            gridPanel.placeComponent(p);
        }
    }

    public void clear() {
        for (PostBanner p : banners) {
            gridPanel.remove(p);
        }

        banners.clear();
        gridPanel.resetCounter();
        gridPanel.showBackground();
    }

    public Set<PostBanner> getBanners() {
        return banners;
    }

    public void removePostIf(Predicate<PostBanner> predicate) {
        PostBanner b = null;
        for (PostBanner banner : banners) {
            if(predicate.test(banner)){
                b = banner;
                gridPanel.remove(b);
            }
        }

        if(b == null) return;

        banners.removeIf(predicate);
        gridPanel.revalidate();
        gridPanel.repaint();

        if (!containsPosts())
            gridPanel.showBackground();
    }

    public PostBanner getPostBanner(String id) {
        return banners.stream().filter(p -> p.getID().equals(id)).findFirst().orElse(null);
    }

    public boolean containsPosts() {
        return !banners.isEmpty();
    }

    public void showBackground() {
        if (banners.isEmpty())
            gridPanel.showBackground();
    }

    public static class BannerLayout extends JPanel {
        private final GridBagConstraints gbc;
        private final JPanel filler = new JPanel();
        private boolean paint = false;
        private int offset = 0;

        public BannerLayout() {
            super(new GridBagLayout());
            gbc = new GridBagConstraints();

            init();
        }

        public void init() {
            gbc.insets = new Insets(5, 200, 5, 200);
            gbc.anchor = GridBagConstraints.FIRST_LINE_START;
            gbc.ipady = 50;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!paint)
                return;

            try {
                BufferedImage img = ImageIO.read(new File("./resources/wallpaper.jpg"));
                Image scaledImg = img.getScaledInstance(getWidth(), getHeight(), Image.SCALE_DEFAULT);
                g.drawImage(scaledImg, 0, 0, null);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void removeBackground() {
            paint = false;
            repaint();
        }

        public void placeComponent(Component comp) {
            remove(filler);
            gbc.gridx = 0;
            gbc.gridy = offset;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            add(comp, gbc);
            offset++;
            placeFiller();
            revalidate();
        }

        public void showBackground() {
            this.paint = true;
            repaint();
        }

        public void placeFiller() {
            gbc.gridy++;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;


            filler.setOpaque(false);
            add(filler, gbc);
        }

        public void resetCounter() {
            this.offset = 0;
        }

        public boolean isBackgroundSetted() {
            return paint;
        }

    }
}
