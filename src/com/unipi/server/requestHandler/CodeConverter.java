package com.unipi.server.requestHandler;

public class CodeConverter {
    public static String convert(String code) {
        switch (code) {
            case "0" -> {
                return "CHANGED LIKE";
            }

            case "200" -> {
                return "OK";
            }

            case "201" -> {
                return "Username già in uso";
            }

            case "202" -> {
                return "Segui già questo utente";
            }

            case "203" -> {
                return "Non è possibile smettere di seguire utenti che non segui";
            }

            case "204" -> {
                return "Formato data non corretto";
            }

            case "205" -> {
                return "Per poter eseguire l operazione, devi prima seguire l utente";
            }

            case "206" -> {
                return "Username non esistente o password errata";
            }

            case "207" -> {
                return "Profilo non esistente";
            }

            case "208" -> {
                return "Post non esistente";
            }

            case "209" -> {
                return "Hai già rewinnato il post";
            }

            case "210" -> {
                return "Profilo o post non esistente";
            }

            case "211" -> {
                return "Non è consentito mettere like/dislike ad un proprio contenuto";
            }

            case "212" -> {
                return "Like già assegnato";
            }

            case "213" -> {
                return "Non è possibile rimuovere Post di altri utenti";
            }

            case "214" -> {
                return "Non è stato possibile aggiornare alcuni utenti";
            }

            case "215" -> {
                return "Devi seguire questo utente prima di visualizzare un suo Post";
            }

            case "216" -> {
                return "Questo rewin non esiste più";
            }

            case "217" -> {
                return "Non puoi eseguire azioni su questo Post, in quanto non segui l autore e non è un rewin di un tuo follow";
            }

            case "300" -> {
                return "Errore sconosciuto. Ci scusiamo per il disagio";
            }


        }

        return String.format("Codice %s non registrato", code);
    }

}
