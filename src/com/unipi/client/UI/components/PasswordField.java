package com.unipi.client.UI.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PasswordField extends JPasswordField {
    private char defaultCharacter = getEchoChar();
    private boolean holderSetted = false;
    private boolean insertedText = false;

    public PasswordField(int limit){
        setColumns(20);
        setFont(new Font("Arial", Font.PLAIN, 22));
        setDocument(new LimitDocument(limit));
        init();
    }

    public void showPassword(){
        setEchoChar((char) 0);
    }
    public void setPlaceHolder(String placeholder){
        initPlaceholder(placeholder);
        holderSetted = true;
    }

    public void setOnKeyEnterPressed(Runnable run){
        for(ActionListener al : getActionListeners())
            removeActionListener(al);

        addActionListener(e -> run.run());
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
    private void initPlaceholder(String placeholder) {
        if(holderSetted)
            return;

        if(new String(getPassword()).isEmpty()){
            showPassword();
            setText(placeholder);
            setForeground(Color.LIGHT_GRAY);
            holderSetted = true;
        }

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if(holderSetted) {
                    hidePassword();
                    setText("");
                    setForeground(Color.BLACK);
                    holderSetted = false;
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (new String(getPassword()).isEmpty() && !holderSetted) {
                    showPassword();
                    setText(placeholder);
                    setForeground(Color.LIGHT_GRAY);
                    holderSetted = true;
                    insertedText = false;
                }else {
                    hidePassword();
                    holderSetted = false;
                }

            }
        });
    }
    public void hidePassword(){
        setEchoChar(defaultCharacter);
    }
    public String getPlainTextPassword(){
        if(!insertedText) return "";

        return new String(getPassword());
    }
}
