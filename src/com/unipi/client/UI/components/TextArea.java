package com.unipi.client.UI.components;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class TextArea extends JTextArea {
    private boolean holderSetted = false;
    private boolean insertedText = false;
    private String placeholder;

    public TextArea(String text, int limit) {
        super(text);

        setLineWrap(true);
        setFont(new Font("Arial", Font.PLAIN, 18));
        setDocument(new LimitDocument(limit));
        init();
    }

    public TextArea(int limit) {
        this("", limit);
    }

    private void init() {
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                insertedText = true;
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }

    private void initPlaceholder(String placeholder) {
        if (holderSetted)
            return;

        if (getText().isEmpty()) {
            setText(placeholder);
            setForeground(Color.LIGHT_GRAY);
            holderSetted = true;
        }

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (holderSetted) {
                    setText("");
                    setForeground(Color.BLACK);
                    holderSetted = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty() && !holderSetted) {
                    setText(placeholder);
                    setForeground(Color.LIGHT_GRAY);
                    holderSetted = true;
                    insertedText = false;
                } else {
                    holderSetted = false;
                }
            }
        });
    }

    public void setPlaceHolder(String placeholder) {
        this.placeholder = placeholder;
        initPlaceholder(placeholder);
    }

    public void clear() {
        if (placeholder == null)
            placeholder = "";

        setText(placeholder);
        setForeground(Color.LIGHT_GRAY);
        insertedText = false;
        holderSetted = true;
    }

    @Override
    public String getText() {
        if (!insertedText) return "";

        Document doc = getDocument();
        String txt;
        try {
            txt = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            txt = null;
        }

        return txt;
    }
}
