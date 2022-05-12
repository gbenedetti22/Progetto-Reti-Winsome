package winsome.common;

import winsome.database.tables.Like;

import java.io.Serializable;

/*
    Versione ridotta della classe Like del DB
 */
public class SimpleLike implements Serializable {
    private Like.TYPE type;

    public SimpleLike(Like.TYPE type) {
        this.type = type;
    }

    public Like.TYPE getType() {
        return type;
    }
}
