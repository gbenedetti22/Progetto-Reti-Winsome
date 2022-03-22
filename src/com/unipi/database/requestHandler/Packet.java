package com.unipi.database.requestHandler;

public class Packet {
    private FUNCTION function;
    private Object message;
    public Packet(FUNCTION function, Object message) {
        this.function = function;
        this.message = message;

        if (message == null)
            this.function = FUNCTION.NONE;
    }

    public static Packet newEmptyPacket() {
        return new Packet(FUNCTION.NONE, null);
    }

    public FUNCTION getFunction() {
        return function;
    }

    public Object getMessage() {
        return message;
    }

    public boolean isEmpty() {
        return function == FUNCTION.NONE && message == null;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "function=" + function +
                ", message=" + message +
                '}';
    }

    public enum FUNCTION {
        DISCOVER,
        GET_FOLLOWERS,
        GET_FOLLOWING,
        FOLLOW,
        UNFOLLOW,
        CREATE_POST,
        CREATE_USER,
        VIEW_POST,
        OPEN_REWIN,
        GET_ALL_POSTS,
        FRIENDS_POSTS,
        REWIN,
        REMOVE_REWIN,
        COMMENT,
        LIKE,
        DISLIKE,
        REMOVE_POST,
        PULL_ENTRIES,
        UPDATE_USER,
        GET_TRANSACTIONS,
        CHECK_IF_EXIST,
        GET_LATEST_POST,
        GET_LATEST_COMMENTS, NONE
    }
}
