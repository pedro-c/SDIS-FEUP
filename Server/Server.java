package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import static Utilities.Constants.MAX_NUMBER_OF_SERVERS;
import static Utilities.Utilities.createHash;

public class Server implements ServerInterface {

    private SSLSocket sslSocket;
    private SSLServerSocket sslServerSocket;
    private SSLSocketFactory sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    private int serverId;
    private String serverIp;
    private int serverPort;
    private String serverIdentifier;

    public Server(String args[]) {

        registerServer();

        this.serverIp = args[0];
        this.serverPort = Integer.parseInt(args[1]);

        this.serverIdentifier = getServerIdentifier();

        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
        } catch (IOException e) {
            System.out.println("Failed to create sslServerSocket");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //For now lets receive a port and a hostname
        if (args.length != 1) {
            throw new IllegalArgumentException("\\nUsage: java server.ServerLauncher<id>");
        }
        Server server = new Server(args);
        server.listen();
    }

    public void listen() {
        while (true) {
            try {
                new ConnectionHandler((SSLSocket) sslServerSocket.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerServer() {

        Random rn = new Random();
        int range = MAX_NUMBER_OF_SERVERS + 1;
        this.serverId = rn.nextInt(range);

        try {
            Registry registry = LocateRegistry.getRegistry();
            try{
                registry.lookup(Integer.toString(this.serverId));
                registerServer();
            }
            catch (NotBoundException e) {
                try {
                    registry.bind(Integer.toString(serverId), this);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println("Failed to bind peer to registry");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to find registry");
        }


    }

    public String getServerIdentifier() {
        return createHash(serverIp + serverPort);
    }

    public class ConnectionHandler implements Runnable {

        private SSLSocket sslSocket;

        public ConnectionHandler(SSLSocket socket) {
            this.sslSocket = socket;
        }

        public void run() {

        }
    }
}
