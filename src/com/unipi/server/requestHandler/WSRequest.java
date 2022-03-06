package com.unipi.server.requestHandler;

import java.io.Serializable;

public class WSRequest implements Serializable {
    public enum WS_OPERATIONS {
        CREATE_USER,
        FIND_USER,
        GET_FRIENDS_BY_TAG,
        GET_FOLLOWERS,
        GET_FOLLOWING,
        FOLLOW,
        UNFOLLOW,
        CREATE_POST,
        GET_POST,
        GET_MY_POSTS,
        GET_FRIENDS_POSTS,
        GET_FRIENDS_POST_FROM_DATE,
        REWIN,
        REMOVE_REWIN,
        COMMENT,
        LIKE,
        DISLIKE,
        REMOVE_POST,
        PULL_NEW_ENTRIES
    }

    private final WS_OPERATIONS op;
    private final Object[] params;

    public WSRequest(WS_OPERATIONS op, Object... params) {
        this.op = op;
        this.params = params;
    }

    public WS_OPERATIONS getOp() {
        return op;
    }

    public Object[] getParams() {
        return params;
    }
}