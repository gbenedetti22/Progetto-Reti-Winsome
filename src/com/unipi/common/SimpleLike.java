package com.unipi.common;

import com.unipi.database.tables.Like;

import java.io.Serializable;

public class SimpleLike implements Serializable {
    private Like.TYPE type;

    public SimpleLike(Like.TYPE type) {
        this.type = type;
    }

    public Like.TYPE getType() {
        return type;
    }
}
