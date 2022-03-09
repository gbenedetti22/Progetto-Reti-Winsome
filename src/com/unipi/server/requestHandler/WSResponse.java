package com.unipi.server.requestHandler;

import java.io.Serializable;

public class WSResponse implements Serializable {
    public enum CODES {
        OK,
        ERROR
    }

    private CODES status;
    private String body = "NONE";

    public WSResponse() {
    }

    public WSResponse(CODES status, String body) {
        this.status = status;
        this.body = body;
    }

    public CODES code() {
        return status;
    }

    public void setStatus(CODES status) {
        if(status == CODES.OK)
            body = "NONE";

        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public static WSResponse newSuccessResponse(){
        return new WSResponse(CODES.OK, "NONE");
    }

    public static WSResponse newSuccessResponse(String message){
        return new WSResponse(CODES.OK, message);
    }

    public static WSResponse newErrorResponse(String message){
        return new WSResponse(CODES.ERROR, message);
    }
}
