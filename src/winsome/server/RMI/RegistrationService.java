package winsome.server.RMI;

import winsome.server.requestHandler.WSRequest;
import winsome.server.requestHandler.WSResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationService extends Remote {
    WSResponse performRegistration(WSRequest request) throws RemoteException;
}
