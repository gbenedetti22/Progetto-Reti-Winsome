package winsome.server.requestHandler;

import java.io.Serializable;

/*
    Classe che simboleggia la risposta di una richiesta da parte di un client.
    status da informazioni sull esito dell operazione (se è andata a buon fine oppure no)
    body rappresenta la risposta in JSON vera e propria (tipo se è stato richiesto di vedere un post vero e proprio, body conterrà il post in JSON)
 */
public class WSResponse implements Serializable {
    private CODES status;
    private String body = "NONE";

    public WSResponse() {
    }

    public WSResponse(CODES status, String body) {
        this.status = status;
        this.body = body;
    }

    public static WSResponse newSuccessResponse() {
        return new WSResponse(CODES.OK, "NONE");
    }

    public static WSResponse newSuccessResponse(String json) {
        return new WSResponse(CODES.OK, json);
    }

    public static WSResponse newErrorResponse(String message) {
        return new WSResponse(CODES.ERROR, message);
    }

    public CODES code() {
        return status;
    }

    public void setStatus(CODES status) {
        if (status == CODES.OK)
            body = "NONE";

        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "WSResponse{" +
                "status=" + status +
                ", body='" + body + '\'' +
                '}';
    }

    public enum CODES {
        OK,
        ERROR
    }
}
