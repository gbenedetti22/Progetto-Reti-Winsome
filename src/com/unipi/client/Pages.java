package com.unipi.client;

import com.unipi.client.UI.banners.UserBanner;
import com.unipi.client.UI.pages.*;

import java.util.ArrayList;

public class Pages {
    public static final LoginPage LOGIN_PAGE = new LoginPage();
    public static final RegisterPage REGISTER_PAGE = new RegisterPage();
    public static final ProfilePage PROFILE_PAGE = new ProfilePage();
    public static final HomePage HOME_PAGE = new HomePage();
    public static final FollowersPage FOLLOW_PAGE = new FollowersPage();
    public static DiscoverPage newDiscoverPage(ArrayList<UserBanner> banners) {
        return new DiscoverPage(banners);
    }
}
