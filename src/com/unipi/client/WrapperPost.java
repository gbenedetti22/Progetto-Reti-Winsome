package com.unipi.client;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/*
    Classe d appoggio per la ricezione di un Post singolo.
    Quando si richiede la visualizzazione di un Post, il Server riponde con un JSON del tipo:
    {
        TITLE: "titolo",
        "CONTENT": "contenuto",
        LIKES: 12
        DISLIKES: 3
        COMMENTS: [
            {
                autore1: "commento1",
            },{
                autore2: "commento2"
            }
        ]

        Quindi, per non usare una HashMap, è stata optata questa soluzione più elegante
 */
public class WrapperPost {
    public String TITLE;
    public String CONTENT;
    public double LIKES;
    public double DISLIKES;
    public List<Map<String, String>> COMMENTS;
}
