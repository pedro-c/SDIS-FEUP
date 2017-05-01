package Server;

import Utilities.Utilities;
import javafx.util.Pair;

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
     * Key is the user id (32-bit hash from e-mail) and value is the 256-bit hashed user password
     */
    private Hashtable<byte[], byte[]> users;
    /**
     * Key is an integer representing the m nodes and the value it's the server identifier
     * (32-bit integer hash from ip+port)
     */
    private HashMap<Integer, String> fingerTable;
    /**
     * Key is the serverId and value is the Pair<Ip,Port>
     */
    private HashMap<String, Pair<String, String>> serversInfo;
    private int serverId;
    private String serverIp;
    private int serverPort;
    private int minIndex;
    private int maxIndex;

    public Server(String args[]) {

        this.serverIp = args[0];
        this.serverPort = Integer.parseInt(args[1]);
        this.fingerTable = new HashMap<>();
        this.serversInfo = new HashMap<>();
        this.serverId = getServerIdentifier();
        initFingerTable();
        saveServerInfoToDisk();
        loadServersInfoFromDisk();
        initServerSocket();
        syncFingerTable();

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

    /**
     * Initiates the server socket for incoming requests
     */
    public void initServerSocket() {
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

    /**
     * Initializes the finger table with m values (max number of nodes = 2^m)
     */
    public void initFingerTable() {
        for (int i = 1; i <= MAX_FINGER_TABLE_SIZE; i++) {
            fingerTable.put(i, null);
        }
    }

    /**
     * Send finger table information to nodes in the finger table
     */
    public void syncFingerTable() {


    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public void serverLookUp(int key) {

        for (Map.Entry<Integer, String> entry : fingerTable.entrySet()) {
            if (Integer.parseInt(entry.getValue()) > key) {
                //forwardRequestToServer(entry.getValue());
            }
        }
        //forwardRequestToServer(fingerTable.get(MAX_FINGER_TABLE_SIZE));

    }

    /**
     * Checks if .config file already has info about this server, if not appends ip:port:id
     */
    public void saveServerInfoToDisk() {
        try {
            File file = new File("./",".config");

            if (!file.isFile() && !file.createNewFile())
            {
                throw new IOException("Error creating new file: " + file.getAbsolutePath());
            }

            BufferedReader reader = new BufferedReader(new FileReader(".config"));
            String line = reader.readLine();
            while (line != null) {
                String[] serverInfo = line.split(":");
                System.out.println(serverId);
                System.out.println(serverInfo[2]);
                if (serverInfo[2].equals(Integer.toString(serverId))) {
                    return;
                }
                line = reader.readLine();
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
                String[] nodeInfo = line.split(":");
                String nodeId = nodeInfo[2];
                String nodeIp = nodeInfo[0];
                String nodePort = nodeInfo[1];
                if (!nodeIp.equals(serverIp)) {
                    int id = Integer.parseInt(nodeId);
                    for (Map.Entry<Integer, String> entry : fingerTable.entrySet()) {
                        /**
                         * succeeder formula = succ(serverId+2^(i-1))
                         *
                         * succeder is a possible node responsible for the values between
                         * the current and the succeder.
                         *
                         * serverId equals to this node position in the circle
                         */
                        int succ = (int) (serverId + Math.pow(2, (entry.getKey() - 1)));
                        /**
                         * if succeder number is bigger than the circle size (max number of nodes)
                         * it starts counting from the beginning
                         * by removing this node position (serverId) from formula
                         */
                        if (succ > Math.pow(2, MAX_FINGER_TABLE_SIZE)) {
                            succ = (int) (Math.pow(2, (entry.getKey() - 1)));
                        }
                        /**
                         * if the succeder is smaller than the value of the node we are reading
                         * from the config file this means that the node we are reading might be
                         * responsible for the keys in between.
                         * If there isn't another node responsible
                         * for this interval or the node we are reading has a smaller value
                         * than the node that used to be responsible for this interval,
                         * than the node we are reading is now the node responsible
                         */
                        if (succ < id) {
                            if (entry.getValue() == null) {
                                fingerTable.put(entry.getKey(), Integer.toString(id));
                                serversInfo.put(nodeId, new Pair<>(nodeIp, nodePort));
                            } else if (id < Integer.parseInt(entry.getValue())) {
                                fingerTable.put(entry.getKey(), Integer.toString(id));
                            }
                        }
                    }
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
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

        System.out.println("Sign up...");

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

        System.out.println("Sign in...");

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
            try {
                in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Error creating buffered reader...");
                e.printStackTrace();
            }
        }

        public void run() {
            String response = null;
            try {
                response = in.readLine();
                analyseResponse(response);
            } catch (IOException e) {
                System.out.println("Error reading line...");
                e.printStackTrace();
            }
        }
    }
}
