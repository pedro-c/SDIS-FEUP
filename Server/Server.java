package Server;

import Utilities.Utilities;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static Utilities.Constants.MAX_FINGER_TABLE_SIZE;
import static Utilities.Utilities.createHash;
import static Utilities.Utilities.get32bitHashValue;

public class Server implements ServerInterface {

    private SSLSocket sslSocket;
    private SSLServerSocket sslServerSocket;
    private SSLSocketFactory sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    private BufferedReader in;
    private PrintWriter out;
    /**
     * Key is the user id (32-bit hash from e-mail) and value is the user password
     */
    private Hashtable<byte[], byte[]> users;
    /**
     * Key is the server identifier (256-bit hash from ip+port) and the value it's the server ip
     */
    private HashMap<Integer, String> fingerTable;
    private int serverId;
    private int nodeID;
    private String serverIp;
    private int serverPort;

    public Server(String args[]) {

        this.serverIp = args[0];
        this.serverPort = Integer.parseInt(args[1]);
        this.fingerTable = new HashMap<>();
        this.serverId = getServerIdentifier();
        this.nodeID = (int) (Math.log(serverId) / Math.log(2));
        initFingerTable();

        loadServersInfoFromDisk();
        saveServerInfoToDisk();

        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
            // TODO: Not working
            // sslServerSocket.setNeedClientAuth(true);
            sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());

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
                System.out.println("Listening...");
                new Thread(new ConnectionHandler((SSLSocket) sslServerSocket.accept())).start();
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

    public void initFingerTable() {
        for (int i = 1; i <= MAX_FINGER_TABLE_SIZE; i++) {
            fingerTable.put(i, null);
        }
    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public void serverLookUp(int key) {


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

                int val = (int) (Math.log(Integer.getInteger(serverInfo[2])) / Math.log(2));
                System.out.println(val);

                for (Map.Entry<Integer, String> entry : fingerTable.entrySet()) {
                    if(nodeID+Math.pow(2,(entry.getKey()-1)) < val){
                        if(entry.getValue() == null){
                            //TODO: change val to serverInfo[2]
                            fingerTable.put(entry.getKey(), Integer.toString(val));
                        }else if(val < Integer.parseInt(entry.getValue())){
                            fingerTable.put(entry.getKey(), Integer.toString(val));
                        }
                    }else{
                        if(entry.getValue() == null){
                            fingerTable.put(entry.getKey(), Integer.toString(val));
                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {

        }
    }

    /**
     * @return Returns 32-bit hash using server ip and server port
     */
    public int getServerIdentifier() {
        return get32bitHashValue(createHash(serverIp + serverPort));
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

    public void analyseResponse(String response) {

        System.out.println(response);
    }

    /**
     * Regists user
     *
     * @param email    user email
     * @param password user password
     */
    public void registUser(String email, String password) {
        if (users.putIfAbsent(createHash(email), createHash(password)) != null)
            System.out.println("Email already exists. Try to sign in instead of sign up...");
        else System.out.println("Signed up with success!");
    }

    /**
     * Authenticates user already registred
     *
     * @param email    user email
     * @param password user password
     * @return true if user authentication wents well, false if don't
     */
    public boolean loginUser(String email, String password) {

        if (!users.containsKey(email)) {
            System.out.println("Try to create an account. Your email was not found on the database...");
            return false;
        }

        if (!users.get(email).equals(Utilities.createHash(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            return false;
        }

        System.out.println("Logged in with success!");

        return true;
    }

    /**
     * Handles new SSL Connections to the server
     */
    public class ConnectionHandler implements Runnable {

        private SSLSocket sslSocket;
        private BufferedReader in;

        public ConnectionHandler(SSLSocket socket) {
            this.sslSocket = socket;
        }

        public void run() {

            System.out.println("Entrei");
            try {
                in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                String response = in.readLine();
                analyseResponse(response);

            } catch (IOException e) {
                System.out.println("Error creating buffered reader...");
                e.printStackTrace();
            }
        }
    }
}
