package com.unipi.server;

import java.util.List;
import java.util.regex.Pattern;


public class ParamsValidator {
    private static final String uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final Pattern uuidPattern = Pattern.compile(uuidRegex);
    private static Pattern specialCharacter = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);

    public static String checkRegisterParams(Object... params) {
        if (params.length != 3) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String)) {
            return "Username non valido";
        }

        if (!(params[1] instanceof String pass)) {
            return "Password non valida";
        }

        if(pass.contains(" ")) {
            return "La password non può contenere caratteri bianchi";
        }

        if (!(params[2] instanceof List<?> list)) {
            return "Lista di tags non valida";
        }

        if (list.size() > 5 || list.size() == 0) return "I tags devono essere al più 5 e minimo 1";

        for (Object o : list) {
            if (!(o instanceof String s)) {
                return "Lista di tags non valida\n" +
                        "Errore nel campo: " + o;
            }

            if(specialCharacter.matcher(s).find() || s.contains(" ")) {
                return "I tags non possono contenere caratteri speciali o spazi bianchi";
            }
        }

        return "OK";
    }

    public static String checkLoginParams(Object[] params) {
        if (params.length != 2) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String)) {
            return "Username non valido";
        }

        if (!(params[1] instanceof String)) {
            return "Password non valida";
        }

        return "OK";
    }

    public static String checkFollowParams(Object[] params) {
        if (params.length != 1) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String)) {
            return "Username dell utente da seguire non valido";
        }

        return "OK";
    }

    public static String checkPublishPostParams(Object[] params) {
        if (params.length != 2) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String title)) {
            return "Titolo non valido";
        }

        if (!(params[1] instanceof String content)) {
            return "Contenuto non valido";
        }

        if(title.isBlank() || content.isBlank()) {
            return "I campi titolo e contenuto non possono essere vuoti!";
        }

        if (((String) params[0]).length() > 20) {
            return "Titolo maggiore di 20 caratteri";
        }

        if (((String) params[1]).length() > 500) {
            return "Testo del Post maggiore di 500 caratteri";
        }

        return "OK";
    }

    public static String checkGetLatestPost(Object[] params) {
        if (params.length != 1) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String)) {
            return "Dati passati non corretti";
        }

        return "OK";
    }

    public static String checkPostActionParams(Object[] params) {
        // le post actions sono tipo: addlIke, addDislike, rewin ecc
        // di fatto, queste azioni hanno tutte gli stessi parametri

        if (params.length != 1) {
            return "Parametri passati non validi";
        }
        if (!(params[0] instanceof String s)) {
            return "Valori inviati non corretti";
        }

        boolean ok = uuidPattern.matcher(s).matches();
        if (!ok) {
            return "ID Post non valido";
        }

        return "OK";
    }

    public static String checkCommentParams(Object[] params) {
        if (params.length != 2) {
            return "Parametri inviati non validi";
        }

        if (!(params[0] instanceof String)) {
            return "ID Post inviato non valido";
        }

        if (!(params[1] instanceof String s)) {
            return "Commento inviato non valido";
        }

        if (s.length() >= 150) {
            return "Commento troppo lungo";
        }

        return "OK";
    }

    public static String checkOpenRewinParams(Object[] params) {
        if (params.length != 2) {
            return "Parametri passati non validi";
        }

        if (!(params[0] instanceof String rewinAuthor)) {
            return "Parametro autore non corretto";
        }

        if (!(params[1] instanceof String idPost)) {
            return "Parametro idPost non corretto";
        }

        boolean ok = uuidPattern.matcher(idPost).matches();
        if (!ok) {
            return "ID Post non valido";
        }

        return "OK";
    }

    public static String checkGetLatestComments(Object[] params) {
        if(params.length != 2) {
            return "Parametri passati non validi";
        }
        if (!(params[0] instanceof String idPost)) {
            return "ID Post inviato non è del tipo giusto";
        }

        if (!(params[1] instanceof String date)) {
            return "Data inviata non valido";
        }

        boolean ok = uuidPattern.matcher(idPost).matches();
        if(!ok) {
            return "ID Post non valido";
        }

        return "OK";
    }
}
