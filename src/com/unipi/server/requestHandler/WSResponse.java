package com.unipi.server.requestHandler;

import java.io.Serializable;

public class WSResponse implements Serializable {
    public enum S_STATUS {
        OK,
        ERROR
    }

    private S_STATUS status;
    private String body = "NONE";

    public WSResponse() {
    }

    public WSResponse(S_STATUS status, String body) {
        this.status = status;
        this.body = body;
    }

    public S_STATUS status() {
        return status;
    }

    public void setStatus(S_STATUS status) {
        if(status == S_STATUS.OK)
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
        return new WSResponse(S_STATUS.OK, "NONE");
    }

    public static WSResponse newSuccessResponse(String message){
        return new WSResponse(S_STATUS.OK, message);
    }

    public static WSResponse newErrorResponse(String message){
        return new WSResponse(S_STATUS.ERROR, message);
    }
}
