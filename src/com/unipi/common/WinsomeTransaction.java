package com.unipi.common;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
    Classe che rappresenta una transazione.
    Quando vengono calcolate le ricompense, viene creata una nuova WinsomeTransaction che conterrà i coins e la data
    di quando è stato effettuato il calcolo delle ricompense
 */
public class WinsomeTransaction implements Comparable<WinsomeTransaction>, Serializable {
    private String coins; // viene usato il tipo string perchè più facile da gestire con i socket
    private String date;

    public WinsomeTransaction(String coins, String date) {
        this.coins = coins;
        this.date = date;
    }

    public String getCoins() {
        return coins;
    }

    public String getDate() {
        return date;
    }

    @Override
    public int compareTo(WinsomeTransaction o) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy - HH:mm:ss");
        try {
            Date d1 = sdf.parse(date);
            Date d2 = sdf.parse(o.getDate());

            return d1.compareTo(d2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
