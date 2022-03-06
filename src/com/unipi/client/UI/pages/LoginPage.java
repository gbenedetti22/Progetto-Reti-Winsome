package com.unipi.client.UI.pages;

import com.unipi.client.UI.components.*;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;
import com.unipi.client.mainFrame.ClientProperties;

import javax.swing.*;
import java.awt.*;

public class LoginPage extends JPanel {
    private final TextInputField username;
    private final PasswordField password;

    public LoginPage(){
        super(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;


        JLabel winsomeLabel = new JLabel("Winsome", SwingConstants.CENTER);
        winsomeLabel.setFont(new Font("Arial", Font.BOLD, 60));

        username = new TextInputField(ClientProperties.USERNAME_MAX_LENGHT);
        username.setPlaceHolder("Username");

        password = new PasswordField(ClientProperties.PASSWORD_MAX_LENGHT);
        password.setPlaceHolder("Password");
        BlueButton button = new BlueButton("Login");
        button.setOnClick(() -> ActionPipe.performAction(ACTIONS.LOGIN_ACTION, new String[]{getUsername(), getPassword()}));

        LinkLabel registerLabel = new LinkLabel("Non sei ancora registrato? Registrati ora!", SwingConstants.CENTER);
        registerLabel.setTextSize(15);
        registerLabel.setOnMouseClick(()->{
            ActionPipe.performAction(ACTIONS.REGISTER_ACTION, null);
        });

        username.setOnKeyEnterKeyPressed(button.getClickAction());
        password.setOnKeyEnterPressed(button.getClickAction());

        gbc.insets = new Insets(0, 0, 35, 0);
        add(winsomeLabel, gbc);
        gbc.insets = new Insets(0, 0, 7, 0);
        add(username, gbc);
        add(password, gbc);
        gbc.ipady=10;
        add(button, gbc);
        gbc.ipady=0;
        gbc.insets = new Insets(10, 0, 0, 0);
        add(registerLabel, gbc);
    }

    public String getUsername(){
        return username.getText();
    }

    public String getPassword(){
        return String.valueOf(password.getPassword());
    }
}
