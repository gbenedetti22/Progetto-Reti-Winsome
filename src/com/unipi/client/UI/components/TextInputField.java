package com.unipi.client.UI.components;

import com.unipi.common.Console;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;

public class TextInputField extends JTextField {
    private boolean holderSetted = false;
    private boolean insertedText = false;
    private String placeholder;

    public TextInputField(String placeHolder) {
        super("");

        setColumns(20);
        setFont(new Font("Arial", Font.PLAIN, 22));
        init();
        setPlaceHolder(placeHolder);
    }

    public TextInputField() {
        this("");
    }

    public TextInputField(String placeholder, int limit) {
        super("");

        setColumns(20);
        setFont(new Font("Arial", Font.PLAIN, 22));
        setDocument(new LimitDocument(limit));
        init();
        setPlaceHolder(placeholder);
    }

    private void init(){
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

    public void setOnKeyEnterKeyPressed(Runnable run){
        for(ActionListener al : getActionListeners())
            removeActionListener(al);

        addActionListener(e -> run.run());
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

    public void clear(){
        if(placeholder == null)
            placeholder = "";

        setText(placeholder);
        setForeground(Color.LIGHT_GRAY);
        insertedText = false;
        holderSetted = true;
    }

    public TextInputField(int limit) {
        this("", limit);
    }

    public void setPlaceHolder(String placeholder) {
        if(placeholder.isEmpty()) return;

        this.placeholder = placeholder;
        initPlaceholder(placeholder);
    }

    @Override
    public String getText() {
        if(!insertedText) return "";

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
