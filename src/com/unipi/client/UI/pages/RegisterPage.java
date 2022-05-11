package com.unipi.client.UI.pages;

import com.unipi.client.UI.components.BlueButton;
import com.unipi.client.UI.components.PasswordField;
import com.unipi.client.UI.components.TextInputField;
import com.unipi.client.mainFrame.ACTIONS;
import com.unipi.client.mainFrame.ActionPipe;
import com.unipi.client.mainFrame.ClientProperties;
import com.unipi.client.mainFrame.MainFrame;
import com.unipi.server.RMI.RegistrationService;
import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import javax.swing.*;
import java.awt.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static com.unipi.client.mainFrame.ClientProperties.NAMES.RMI_ADDRESS;
import static com.unipi.client.mainFrame.ClientProperties.NAMES.RMI_REG_PORT;

public class RegisterPage extends JPanel {
    private TextInputField usernameField;
    private PasswordField passwordField;
    private PasswordField repeatPassword;
    private ArrayList<TextInputField> tags;
    private RegistrationService service;

    public RegisterPage() {
        super(new BorderLayout());
        this.tags = new ArrayList<>();

        JPanel formPanel = getForm();
        JPanel tagsPanel = new JPanel();

        for (int i = 1; i <= 5; i++) {
            TextInputField tag = new TextInputField(10);
            tag.setPlaceHolder("tags" + i);
            tagsPanel.add(tag);
            tags.add(tag);
        }

        tagsPanel.setPreferredSize(new Dimension(100, 100));
        add(formPanel, BorderLayout.CENTER);
        add(tagsPanel, BorderLayout.SOUTH);

        HashMap<ClientProperties.NAMES, Object> props = ClientProperties.getValues();
        try {
            Registry registry = LocateRegistry.getRegistry((String) props.get(RMI_ADDRESS), (int) props.get(RMI_REG_PORT));
            this.service = (RegistrationService) registry.lookup("REGISTER-SERVICE");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private JPanel getForm() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;

        JLabel usernameLabel = new JLabel("Inserisci Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        formPanel.add(usernameLabel, gbc);

        usernameField = new TextInputField(16);
        usernameField.setPlaceHolder("Username");
        formPanel.add(usernameField, gbc);

        gbc.insets = new Insets(10, 0, 0, 10);
        JLabel passwordLabel = new JLabel("Inserisci Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        formPanel.add(passwordLabel, gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        passwordField = new PasswordField(20);
        passwordField.setPlaceHolder("Password");
        formPanel.add(passwordField, gbc);

        gbc.insets = new Insets(10, 0, 0, 10);
        JLabel repeatPasswordLabel = new JLabel("Ripeti Password");
        repeatPasswordLabel.setFont(new Font("Arial", Font.PLAIN, 15));
        formPanel.add(repeatPasswordLabel, gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        repeatPassword = new PasswordField(20);
        repeatPassword.setPlaceHolder("Ripeti Password");
        formPanel.add(repeatPassword, gbc);

        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.ipady = 10;

        BlueButton button = new BlueButton("Registrati");
        button.setOnClick(this::registerAction);
        formPanel.add(button, gbc);
        return formPanel;
    }

    // metodo per eseguire la registrazione tramite RMUìI
    // i campi vengono controllati lato client e lato Server
    private void registerAction() {
        try {
            String username = usernameField.getText();
            String password1 = passwordField.getPlainTextPassword();
            String password2 = repeatPassword.getPlainTextPassword();

            if (username.contains(" ")) {
                MainFrame.showErrorMessage("Username non valido. Non può contenere spazi.");
                return;
            }

            if (password1.isEmpty()) {
                JOptionPane.showMessageDialog(null, "La password non può essere vuota", "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password1.equals(password2)) {
                JOptionPane.showMessageDialog(null, "Le password non corrispondono", "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ArrayList<String> tagsList = new ArrayList<>(5);
            for (TextInputField input : tags) {
                String tag = input.getText();
                if (!tag.isBlank()) {
                    tagsList.add(tag);
                }
            }
            if (tagsList.isEmpty()) {
                MainFrame.showErrorMessage("Devi inserire al più un tag");
                return;
            }

            tagsList.sort(Comparator.naturalOrder());
            for (int i = 0; i < tagsList.size() - 1; i++) {
                if (tagsList.get(i).equals(tagsList.get(i + 1))) {
                    MainFrame.showErrorMessage("Tag uguali non ammessi");
                    return;
                }
            }

            WSRequest request = new WSRequest(WSRequest.WS_OPERATIONS.CREATE_USER, username, password1, tagsList);
            WSResponse response = service.performRegistration(request);

            if (response.code() != WSResponse.CODES.OK) {
                JOptionPane.showMessageDialog(null, response.getBody(), "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ActionPipe.performAction(ACTIONS.SWITCH_PAGE, new LoginPage());
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Errore nella connessione al Server", "Errore",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearFields() {
        usernameField.clear();
        passwordField.clear();
        repeatPassword.clear();
        for (TextInputField tagInput : tags) {
            tagInput.clear();
        }
    }
}
