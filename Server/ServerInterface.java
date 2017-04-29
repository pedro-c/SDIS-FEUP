package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote{

    String getServerIp() throws RemoteException;

    int getServerPort() throws RemoteException;

}
