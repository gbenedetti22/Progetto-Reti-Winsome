package com.unipi.client.mainFrame;

import com.unipi.client.Pages;
import com.unipi.client.UI.pages.LoginPage;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.io.IOException;
import java.rmi.NotBoundException;

//TODO: mettere i thread Pool
public class MainFrame extends JFrame {
    private MainFrameThread mainThread;
    private JPanel currentPage; //la prima pagina è il Login (vedi costruttore)
    private JPanel previousPage;
    private final JProgressBar progressBar;
    private boolean progressBarOn = false;

    public MainFrame() {
        try {
            mainThread = new MainFrameThread(this);
        } catch (IOException | NotBoundException e ) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Impossibile collegarsi al Server", "Errore", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        mainThread.start();
        currentPage = Pages.LOGIN_PAGE;
        previousPage = null;
        progressBar = defaultProgressBar();

        setTitle("Winsome - Social Network");
        setMinimumSize(new Dimension(1280, 720));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(new Dimension(1280, 720));
        setLocationRelativeTo(null);
        add(currentPage);
        pack();
        setVisible(true);
    }

    private JProgressBar defaultProgressBar() {
        JProgressBar progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(progressBar.getWidth(), 40));
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(0xabffd8));
        progressBar.setBackground(new Color(0x828282));
        progressBar.setUI(new BasicProgressBarUI() {
            protected Color getSelectionBackground() {
                return Color.WHITE;
            }

            protected Color getSelectionForeground() {
                return new Color(0x707070);
            }
        });
        progressBar.setFont(new Font("Arial", Font.PLAIN, 18));
        return progressBar;
    }

    public JProgressBar getProgressBar() {
        return progressBar;
    }

    public synchronized void switchPage(JPanel page) {
        setMaximized(!(page instanceof LoginPage));

        getContentPane().removeAll();
        getContentPane().add(page);
        revalidate();
        repaint();

        previousPage = currentPage;
        currentPage = page;
        if (progressBarOn)
            showProgressBar();
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

    public void showProgressBar() {
        add(progressBar, BorderLayout.PAGE_END);
        revalidate();
        repaint();
        progressBarOn = true;
    }

    public void hideProgressBar() {
        remove(progressBar);
        revalidate();
        repaint();
        progressBarOn = false;
    }


    public JPanel getPreviousPage() {
        return previousPage;
    }

    public void goBack() {
        switchPage(previousPage);
    }
}
