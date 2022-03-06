package com.unipi.server.RMI;

import com.unipi.server.requestHandler.WSRequest;
import com.unipi.server.requestHandler.WSResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationService extends Remote {
    WSResponse performRegistration(WSRequest request) throws RemoteException;
}
