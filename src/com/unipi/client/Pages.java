package com.unipi.client;

import com.unipi.client.UI.pages.*;

public class Pages {
    public static final LoginPage LOGIN_PAGE = new LoginPage();
    public static final RegisterPage REGISTER_PAGE = new RegisterPage();
    public static final ProfilePage PROFILE_PAGE = new ProfilePage();
    public static final HomePage HOME_PAGE = new HomePage();
    public static final FollowersPage FOLLOW_PAGE = new FollowersPage();
    public static final DiscoverPage DISCOVER_PAGE = new DiscoverPage();
    public static PostPage newPostPage(String id, String title, String content) {
        return new PostPage(id, title, content);
    }
}
