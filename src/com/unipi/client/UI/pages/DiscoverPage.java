package com.unipi.client.UI.pages;

import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.components.*;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class DiscoverPage extends JPanel {
    private LinkLabel label;

    public DiscoverPage(ArrayList<UserBanner> banners) {
        super(new BorderLayout());
        label = new LinkLabel();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.ipady = 50;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, gbc);
        gbc.gridy++;
        for (UserBanner banner : banners) {
            panel.add(banner, gbc);
            gbc.gridy++;
        }

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

        label = new LinkLabel("Avanti");
        label.setMargin(new Insets(0, 0, 0, 10));
        label.setTextSize(19);

        JPanel topBarPanel = new JPanel(new BorderLayout());
        topBarPanel.add(inputField, BorderLayout.LINE_START);
        topBarPanel.add(label, BorderLayout.LINE_END);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(15);

        add(topBarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    public void setNextHope(ACTIONS action){
        if(action != ACTIONS.HOME_ACTION && action != ACTIONS.PROFILE_ACTION) {
            label.setMyAction(ACTIONS.NONE, "None");
            return;
        }

        if(action == ACTIONS.HOME_ACTION) {
            label.setMyAction(action, "Avanti");
            label.setOnMouseClick(()->{
                ActionPipe.performAction(ACTIONS.HOME_ACTION, null);
            });
        }else {
            label.setMyAction(action, "Il mio Profilo");
            label.setOnMouseClick(()->{
                ActionPipe.performAction(ACTIONS.PROFILE_ACTION, null);
            });
        }
    }
}
