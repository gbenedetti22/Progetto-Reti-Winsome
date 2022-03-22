package com.unipi.client.UI.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class BlueButton extends JButton {
    private Runnable clickAction;

    public BlueButton(String text) {
        super(text);
        setBackground(new Color(0x007bff));
        setFont(new Font("Arial", Font.PLAIN, 17));
        setFocusPainted(false);
        setForeground(Color.WHITE);
    }

    public void setOnClick(Runnable runnable) {
        for (ActionListener listener : getActionListeners())
            removeActionListener(listener);

        addActionListener(e -> runnable.run());
        this.clickAction = runnable;
    }

    public Runnable getClickAction() {
        return clickAction;
    }
}
