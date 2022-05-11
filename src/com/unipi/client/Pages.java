package com.unipi.client;

import com.unipi.client.UI.pages.*;

/*
    Classe che racchiude tutte le pagine.
    Per motivi di performance e correttezza, le pagine vengono riusate.
    L unica classe che viene creata ogni volta è quella che serve a visualizzare un Post (per via dei like e dei commenti)
 */
public class Pages {
    public static final LoginPage LOGIN_PAGE = new LoginPage();
    public static final RegisterPage REGISTER_PAGE = new RegisterPage();
    public static final ProfilePage PROFILE_PAGE = new ProfilePage();
    public static final HomePage HOME_PAGE = new HomePage();
    public static final FollowersPage FOLLOW_PAGE = new FollowersPage();
    public static final DiscoverPage DISCOVER_PAGE = new DiscoverPage();

    public static PostPage newPostPage(String id, String author, String title, String content) {
        return new PostPage(id, author, title, content);
    }

    public static void resetPages() {
        LOGIN_PAGE.clearFields();
        HOME_PAGE.clear();
        PROFILE_PAGE.clear();
        FOLLOW_PAGE.clear();
        DISCOVER_PAGE.clear();
    }
}
