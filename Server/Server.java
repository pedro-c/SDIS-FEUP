package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;

import static Utilities.Constants.MAX_FINGER_TABLE_SIZE;
import static Utilities.Utilities.createHash;
import static Utilities.Utilities.getBigInteger;

public class Server implements ServerInterface {

    private SSLSocket sslSocket;
    private SSLServerSocket sslServerSocket;
    private SSLSocketFactory sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    private Hashtable<BigInteger, String> fingerTable;
    private BigInteger serverId;
    private String serverIp;
    private int serverPort;

    public Server(String args[]) {

        this.serverIp = args[0];
        this.serverPort = Integer.parseInt(args[1]);
        this.fingerTable = new Hashtable<>();
        this.serverId = getServerIdentifier();

        loadServersInfoFromDisk();
        saveServerInfoToDisk();

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
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java server.Server<ip,port>");
        }
        Server server = new Server(args);
        server.listen();
    }

    /**
     * Listens for incoming connection requests
     */
    public void listen() {
        while (true) {
            try {
                new ConnectionHandler((SSLSocket) sslServerSocket.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers server to RMI
     */
    public void registerServer() {

        //TODO: Do we really need RMI? It's not scalable on the internet
        /*
        try {
            Registry registry = LocateRegistry.getRegistry();
            try{
                registry.lookup(this.serverId.toString());
                registerServer();
            }
            catch (NotBoundException e) {
                try {
                    registry.rebind(serverId.toString(), this);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println("Failed to bind peer to registry");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to find registry");
        }*/


    }

    /**
     * Checks if .config file already has info about this server, if not appends ip:port:id
     */
    public void saveServerInfoToDisk() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(".config"));
            String line = reader.readLine();
            while (line != null) {
                String[] serverInfo = line.split(":");
                if (serverInfo[2].equals(serverId))
                    return;
            }
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(".config", true)));
            out.println(serverIp + ":" + serverPort + ":" + serverId);
            out.close();
            System.out.println("Saved server info to config file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets servers info from .config file and loads the finger table with closest preceding servers
     */
    public void loadServersInfoFromDisk() {

        try (BufferedReader reader = new BufferedReader(new FileReader(".config"))) {
            String line = reader.readLine();

            while (line != null) {
                String[] serverInfo = line.split(":");

                if (fingerTable.size() < MAX_FINGER_TABLE_SIZE) {
                    fingerTable.put(new BigInteger(serverInfo[2]), serverInfo[0]);
                } else {
                    Enumeration<BigInteger> servers = fingerTable.keys();
                    while (servers.hasMoreElements()) {
                        BigInteger server = servers.nextElement();

                        //TODO: Added closest preceding servers to finger table instead of all
                        if (server.compareTo(new BigInteger(serverInfo[2])) == 1) {

                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {

        }
    }

    /**
     * @return Returns 256-bit hash using server ip and server port
     */
    public BigInteger getServerIdentifier() {
        return getBigInteger(createHash(serverIp + serverPort));
    }

    /**
     * @return Returns server ip address
     */
    public String getServerIp() {
        return this.serverIp;
    }

    /**
     * @return Returns server port
     */
    public int getServerPort() {
        return this.serverPort;
    }

    /**
     * Handles new SSL Connections to the server
     */
    public class ConnectionHandler implements Runnable {

        private SSLSocket sslSocket;

        public ConnectionHandler(SSLSocket socket) {
            this.sslSocket = socket;
        }

        public void run() {

        }
    }
}
