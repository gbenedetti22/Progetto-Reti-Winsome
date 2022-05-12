package winsome.server.requestHandler;

import java.io.Serializable;
import java.util.Arrays;

public class WSRequest implements Serializable {
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

    @Override
    public String toString() {
        return "WSRequest{" +
                "op=" + op +
                ", params=" + Arrays.toString(params) +
                '}';
    }

    public enum WS_OPERATIONS {
        CREATE_USER,
        LOGIN,
        GET_FRIENDS_BY_TAG,
        GET_FOLLOWERS,
        GET_FOLLOWING,
        FOLLOW,
        UNFOLLOW,
        CREATE_POST,
        GET_POST,
        OPEN_REWIN,
        GET_MY_POSTS,
        GET_FRIENDS_POSTS,
        GET_FRIENDS_POST_FROM_DATE,
        REWIN,
        REMOVE_REWIN,
        COMMENT,
        LIKE,
        DISLIKE,
        REMOVE_POST,
        PULL_NEW_ENTRIES,
        GET_TRANSACTIONS,
        GET_COMMENTS_FROM_DATE, LOGOUT
    }
}
