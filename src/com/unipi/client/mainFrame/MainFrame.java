package com.unipi.client.mainFrame;

import com.unipi.client.Pages;
import com.unipi.client.UI.pages.LoginPage;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.NotBoundException;

/*
    Classe che rappresenta il punto di interscambio tra le pagine.
    Si può pensare a questo oggetto come ad una activity nella programmazione Android (mentre i pannelli sono i Fragment)
    Per più informazioni, vedere la relazione
 */
public class MainFrame extends JFrame {
    private MainFrameThread mainThread;
    private JPanel currentPage; //la prima pagina è il Login (vedi costruttore)
    private JPanel previousPage;    // corrisponde alla pagina precedente.
    // Se l utente va dalla Home al Profilo, la pagina precedente sarà la Home

    public MainFrame() {
        try {
            mainThread = new MainFrameThread(this);
        } catch (IOException | NotBoundException e) {
            JOptionPane.showMessageDialog(null, "Impossibile collegarsi al Server", "Errore", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        mainThread.start();
        currentPage = Pages.LOGIN_PAGE;
        previousPage = null;

        setTitle("Winsome - Social Network");
        setMinimumSize(new Dimension(1280, 720));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        add(currentPage);
        pack();
        setVisible(true);
    }

    public static void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(null,
                msg,
                "Errore", JOptionPane.ERROR_MESSAGE);
    }

    public static void showSuccessMessage(String msg) {
        ImageIcon icon = new ImageIcon("./resources/checked.png");
        Image image = icon.getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
        icon = new ImageIcon(image);

        JOptionPane.showMessageDialog(null, msg, "", JOptionPane.PLAIN_MESSAGE, icon);
    }


    // Metodo per cambiare pagina. Viene tenuto traccia della pagina precedente nel caso in cui l utente decidesse
    // di tornare indietro
    public synchronized void switchPage(JPanel page) {
        setMaximized(!(page instanceof LoginPage));

        getContentPane().removeAll();
        getContentPane().add(page);
        revalidate();
        repaint();

        previousPage = currentPage;
        currentPage = page;
    }

    public synchronized JPanel getCurrentPage() {
        return currentPage;
    }

    public void setMaximized(boolean value) {
        if (value)
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        else {
            setSize(1280, 720);
            setLocationRelativeTo(null);
            setExtendedState(getExtendedState() | JFrame.NORMAL);
        }
    }

    public void goBack() {
        switchPage(previousPage);
    }
}
