package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.UI.components.TextInputField;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;

public class DiscoverPage extends JPanel {
    private final JPanel filler = new JPanel();
    private JPanel panel;
    private GridBagConstraints gbc;
    private int offset;
    private LinkedHashSet<UserBanner> banners;

    public DiscoverPage(ArrayList<UserBanner> banners) {
        super(new BorderLayout());
        this.banners = new LinkedHashSet<>();
        this.banners.addAll(banners);

        panel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.ipady = 50;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        for (int i = 0; i < banners.size(); i++) {
            panel.add(banners.get(i), gbc);
            gbc.gridy = i;
        }
        offset = 0;

        TextInputField inputField = getSearchField();

        LinkLabel label = new LinkLabel("Indietro");
        label.setMargin(new Insets(0, 0, 0, 10));
        label.setTextSize(19);
        label.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.BACKPAGE_ACTION, null));

        JPanel topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.add(inputField, BorderLayout.LINE_START);
        topBarPanel.add(label, BorderLayout.LINE_END);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        add(topBarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public DiscoverPage() {
        this(new ArrayList<>());
    }

    private TextInputField getSearchField() {
        TextInputField inputField = new TextInputField(20);
        inputField.setPlaceHolder("Cerca");
        inputField.setMargin(new Insets(10, 10, 10, 10));

        inputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                for (UserBanner banner : banners) {
                    String input = inputField.getText();
                    boolean contains = banner.getUsername().contains(input);

                    if (contains && !banner.isVisible()) {
                        banner.setVisible(true);
                    } else if (!contains) {
                        banner.setVisible(false);
                    }
                }
            }
        });
        return inputField;
    }

    public void add(UserBanner banner) {
        boolean added = banners.add(banner);
        if (!added) return;

        panel.remove(filler);
        gbc.gridy = offset;
        gbc.weighty = 0;
        panel.add(banner, gbc);
        offset++;
        placeFiller();
        panel.revalidate();
    }

    public void addAll(ArrayList<UserBanner> banners) {
        clear();
        for (UserBanner b : banners)
            add(b);
    }

    public void clear() {
        offset = 0;
        for (UserBanner banner : banners)
            panel.remove(banner);

        banners.clear();
    }

    private void placeFiller() {
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        filler.setOpaque(false);
        panel.add(filler, gbc);
        panel.revalidate();

        gbc.fill = GridBagConstraints.HORIZONTAL;
    }


}