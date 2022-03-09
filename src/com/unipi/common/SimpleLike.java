package com.unipi.common;

import com.unipi.database.tables.Like;

import java.io.Serializable;

public class SimpleLike implements Serializable {
    private Like.type type;

    public SimpleLike(Like.type type) {
        this.type = type;
    }

    public Like.type getType() {
        return type;
    }
}
