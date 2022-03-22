package com.unipi.database;

import java.io.Serializable;

public class DBResponse implements Serializable {
    private String code;
    private Object message;

    public DBResponse(String code, Object message) {
        this.code = code;
        this.message = message;
    }

    public DBResponse(String code) {
        this.code = code;
        this.message = null;
    }

    public String getCode() {
        return code;
    }

    public Object getMessage() {
        return message;
    }


    @Override
    public String toString() {
        return "DBResponse{" +
                "code='" + code + '\'' +
                ", message=" + message +
                '}';
    }
}
