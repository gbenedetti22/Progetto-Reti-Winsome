package com.unipi.client.UI.components;

import com.unipi.client.mainFrame.ACTIONS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class LinkLabel extends JLabel {
    private ACTIONS myAction;
    private Runnable clickAction;

    public LinkLabel(String text){
        setMyAction(ACTIONS.NONE, text);

        setAlignmentX(LEFT_ALIGNMENT);
        setFont(new Font("Arial", Font.PLAIN, 15));
        setForeground(Color.BLUE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public LinkLabel(){
        this("");
    }

    public LinkLabel(String text, int alignment){
        super(text, alignment);
        setMyAction(ACTIONS.NONE, text);
        setFont(new Font("Arial", Font.PLAIN, 15));
        setForeground(Color.BLUE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setMargin(Insets insets){
        setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right));
    }

    public void setTextSize(int size){
        setFont(new Font("Arial", Font.PLAIN, size));
    }
    public void setOnMouseClick(Runnable runnable){
        for(MouseListener listener : getMouseListeners())
            removeMouseListener(listener);

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
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

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });
        this.clickAction = runnable;
    }

    public void setMyAction(ACTIONS action, String text) {
        if(!text.startsWith("<html>"))
            text = String.format("<html><u>%s</u></html>", text);

        myAction = action;
        setText(text);
    }

}
