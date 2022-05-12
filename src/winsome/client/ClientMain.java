package winsome.client;

import winsome.client.mainFrame.MainFrame;

import javax.swing.*;

public class ClientMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
