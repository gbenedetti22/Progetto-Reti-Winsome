package com.unipi.client.UI.pages;

import com.unipi.client.Pages;
import com.unipi.client.UI.banners.PostBanner;
import com.unipi.client.UI.components.BlueButton;
import com.unipi.client.UI.components.LinkLabel;
import com.unipi.client.UI.components.TextArea;
import com.unipi.client.UI.components.TextInputField;
import com.unipi.client.mainFrame.*;
import com.unipi.common.WinsomeTransaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.function.Predicate;

public class ProfilePage extends JPanel {
    private final HomePage.BannerLayout postLayout;
    private LinkLabel winsomeCoinsLabel;
    private LinkLabel bitcoinslabel;
    private TextInputField titleInput;
    private TextArea postContentInput;
    private String newPostTitle;
    private String newPostContent;
    private JLabel usernameLabel;
    private LinkedHashSet<PostBanner> banners;
    private LinkedList<WinsomeTransaction> transactions;
    private RandomORG randomORG;
    private float winsomeCoins = 0;

    public ProfilePage(String username) {
        super(new BorderLayout());
        this.banners = new LinkedHashSet<>();
        this.randomORG = new RandomORG();

        JPanel topPanel = initTopPanel(username);
        JScrollPane bannerLayout = HomePage.createScrollableBannerLayout(10);
        postLayout = (HomePage.BannerLayout) bannerLayout.getViewport().getView();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(516);
        splitPane.setDividerSize(0);

        splitPane.add(topPanel, JSplitPane.TOP);
        splitPane.add(bannerLayout, JSplitPane.BOTTOM);

        add(splitPane, BorderLayout.CENTER);
    }

    public ProfilePage() {
        this("");
    }

    private JPanel initTopPanel(String username) {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel dashboard = new JPanel();
        dashboard.setLayout(new GridBagLayout());
        dashboard.setBorder(new EmptyBorder(50, 50, 50, 50));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        usernameLabel = new JLabel(username);
        usernameLabel.setHorizontalAlignment(JLabel.HORIZONTAL);
        usernameLabel.setFont(new Font("Arial", Font.BOLD, 27));

        winsomeCoinsLabel = new LinkLabel(0 + " WinsomeCoins");
        winsomeCoinsLabel.setHorizontalAlignment(JLabel.LEFT);
        winsomeCoinsLabel.setFont(new Font("Arial", Font.PLAIN, 17));
        winsomeCoinsLabel.setForeground(Color.BLACK);

        bitcoinslabel = new LinkLabel(0 + " Bitcoins");
        bitcoinslabel.setHorizontalAlignment(JLabel.LEFT);
        bitcoinslabel.setFont(new Font("Arial", Font.PLAIN, 17));
        bitcoinslabel.setForeground(new Color(0xE99600));
        bitcoinslabel.setOnMouseClick(() -> {
            bitcoinslabel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int randomNumber = randomORG.getRandomNumber();
            if (randomNumber == -1) {
                MainFrame.showErrorMessage("Impossibile reperire il tasso di cambio attuale.\n" +
                        "Riprovare più tardi.");
            }
            bitcoinslabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            float n = randomNumber == -1 ? randomNumber : (randomNumber * winsomeCoins);
            bitcoinslabel.setText(n + " Bitcoins");
        });

        LinkLabel historyLabel = new LinkLabel("Cronologia Operazioni");

        historyLabel.setOnMouseClick(() -> {
            HistoryWallet wallet = new HistoryWallet(transactions);
            wallet.open();
        });

        titleInput = new TextInputField(ClientProperties.POST_TITLE_MAX_LENGHT);
        titleInput.setPlaceHolder("Titolo");

        postContentInput = new TextArea(ClientProperties.POST_CONTENT_MAX_LENGHT);
        postContentInput.setPlaceHolder("A costa stai pensando");
        BlueButton button = new BlueButton("Pubblica");
        button.setOnClick(() -> {
            newPostTitle = titleInput.getText();
            newPostContent = postContentInput.getText();
            ActionPipe.performAction(ACTIONS.PUBLISH_ACTION, null);

            titleInput.clear();
            postContentInput.clear();
        });

        LinkLabel back = new LinkLabel("Home", SwingConstants.LEFT);
        back.setTextSize(16);
        back.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.SWITCH_PAGE, Pages.HOME_PAGE));

        LinkLabel followers = new LinkLabel("I miei Followers", SwingConstants.RIGHT);
        followers.setOnMouseClick(() -> {
            ActionPipe.performAction(ACTIONS.FOLLOWERS_PAGE_ACTION, null);
        });

        LinkLabel addUsersLabel = new LinkLabel("Cerca nuovi utenti", SwingConstants.RIGHT);
        addUsersLabel.setMargin(new Insets(0, 0, 0, 20));
        addUsersLabel.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.DISCOVER_ACTION, null));

        JPanel topRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topRightPanel.setBackground(Color.WHITE);
        topRightPanel.add(addUsersLabel);
        topRightPanel.add(followers);

        postContentInput.setLineWrap(true);
        titleInput.setColumns(30);

        LinkLabel logout = new LinkLabel("Logout");
        logout.setHorizontalAlignment(JLabel.HORIZONTAL);
        logout.setOnMouseClick(() -> ActionPipe.performAction(ACTIONS.LOGOUT_ACTION, null));

        dashboard.add(usernameLabel, gbc);
        dashboard.add(logout, gbc);
        dashboard.add(winsomeCoinsLabel, gbc);
        dashboard.add(bitcoinslabel, gbc);
        dashboard.add(historyLabel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 3;
        dashboard.add(titleInput, gbc);

        gbc.weighty = 1;
        dashboard.add(postContentInput, gbc);

        gbc.weighty = 0;
        gbc.insets = new Insets(13, 5, 5, 5);
        dashboard.add(button, gbc);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(back, BorderLayout.LINE_START);
        topBar.add(topRightPanel, BorderLayout.LINE_END);
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        topPanel.add(topBar, BorderLayout.NORTH);
        topPanel.add(dashboard, BorderLayout.CENTER);
        topBar.setBackground(Color.WHITE);

        return topPanel;
    }

    public void addPost(PostBanner banner) {
        postLayout.placeComponent(banner);
        banners.add(banner);
    }

    public void removePost(PostBanner banner) {
        postLayout.remove(banner);
        banners.remove(banner);
        revalidate();
        repaint();
    }

    public void removePostIf(Predicate<PostBanner> predicate) {
        PostBanner b = null;
        for (PostBanner banner : banners) {
            if (predicate.test(banner)) {
                b = banner;
                postLayout.remove(b);
            }
        }

        if (b == null) return;

        banners.removeIf(predicate);
        postLayout.revalidate();
        postLayout.repaint();
    }

    public String getNewPostTitle() {
        return newPostTitle;
    }

    public String getNewPostContent() {
        return newPostContent;
    }

    public void setUsername(String username) {
        usernameLabel.setText(username);
    }

    public boolean containsPost() {
        return !banners.isEmpty();
    }

    public void setWinsomeCoins(String n) {
        winsomeCoinsLabel.setText(n + " WinsomeCoins");
    }

    public void setTransactions(LinkedList<WinsomeTransaction> transactions) {
        this.transactions = transactions;

        float n = 0;
        for (WinsomeTransaction transaction : transactions) {
            n += Float.parseFloat(transaction.getCoins());
        }

        this.winsomeCoins = n;
        winsomeCoinsLabel.setText(n + " WinsomeCoins");
    }

    public void clear() {
        for (PostBanner banner : banners) {
            postLayout.remove(banner);
        }

        banners.clear();
        postLayout.resetCounter();
        setWinsomeCoins("0");
        bitcoinslabel.setText("0 Bitcoins");
        setUsername(null);
        transactions.clear();
        titleInput.clear();
        postContentInput.clear();
    }
}
