package com.unipi.database;

import java.io.Serializable;

/*
    Classe che rappresenta una risposta del DB.
    Questa classe contiene un codice (che rappresenta lo stato della risposta) e il messaggio.

    Es.
    Quando il Server invia una richiesta di "FIND USER" per il login, se l operazione va a buon fine, riceverà:
    - code -> 200
    - message -> lista dei tag con cui si è registrato
 */
public class DBResponse implements Serializable {
    public static final String OK = "200";
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
