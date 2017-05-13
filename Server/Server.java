package Server;

import Messages.Message;
import Messages.MessageHandler;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

public class Server extends Node {

    /**
     * Key is the user id (hash from e-mail) and value is the 256-bit hashed user password
     */
    private Hashtable<BigInteger, User> users;
    /**
     * Key is an integer representing the m nodes and the value it's the server identifier
     * (32-bit integer hash from ip+port)
     */
    private Node[] fingerTable = new Node[MAX_FINGER_TABLE_SIZE + 1];
    private SSLServerSocket sslServerSocket;
    private SSLServerSocketFactory sslServerSocketFactory;
    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private Node predecessor = this;

    public Server(String args[]) {
        super(args[0], args[1]);

        initFingerTable();
        //saveServerInfoToDisk();
        //loadServersInfoFromDisk();
        initServerSocket();

        if(args.length>3){
            Node knownNode = new Node(args[2],args[3]);
            joinNetwork(knownNode);
        }

        users = new Hashtable<>();
    }

    /**
     *
     * @param args [serverIp] [serverPort] [knownServerIp] [knownServerPort]
     */
    public static void main(String[] args) {
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
                ConnectionHandler handler = new ConnectionHandler((SSLSocket) sslServerSocket.accept(), this);
                threadPool.submit(handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initiates the server socket for incoming requests
     */
    public void initServerSocket() {
        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(Integer.parseInt(this.getNodePort()));
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
            fingerTable[i] = this;
        }
    }

    /**
     * Sends a message to the network
     */
    public void joinNetwork(Node knownNode) {

        Message message = new Message(NEWNODE.getBytes(), Integer.toString(this.getNodeId()), Integer.toString(predecessor.getNodeId()), predecessor.getNodeIp(), predecessor.getNodePort());

        MessageHandler handler = new MessageHandler(message, knownNode.getNodeIp(), knownNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);
        handler.receiveResponse();

    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public int serverLookUp(int key) {
        int id = this.getNodeId();
        for (int i = 0; i < fingerTable.length; i++) {
            id = fingerTable[i].getNodeId();
            if (id > key) {
                return id;
            }
        }
        return id;
    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     * and returns its predecessor
     *
     * @param key 32-bit identifier
     * @return Predecessor node
     */
    public Node predecessorLookUp(int key){
        Node id = this;
        for (int i = 0; i < fingerTable.length; i++) {
            id = fingerTable[i];
            if (id.getNodeId() > key) {
                return this.predecessor;
            }
        }
        return id;
    }

    /**
     * Checks if .config file already has info about this server, if not appends ip:port:id
     */
    public void saveServerInfoToDisk() {
        try {
            File file = new File("./", ".config");

            if (!file.isFile() && !file.createNewFile()) {
                throw new IOException("Error creating new file: " + file.getAbsolutePath());
            }

            BufferedReader reader = new BufferedReader(new FileReader(".config"));
            String line = reader.readLine();
            while (line != null) {
                String[] serverInfo = line.split(":");
                System.out.println(this.getNodeId());
                System.out.println(serverInfo[2]);
                if (serverInfo[2].equals(Integer.toString(this.getNodeId()))) {
                    return;
                }
                line = reader.readLine();
            }
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(".config", true)));
            out.println(this.getNodeIp() + ":" + this.getNodePort() + ":" + this.getNodeId());
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
                if (!nodeIp.equals(this.getNodeIp())) {
                    int id = Integer.parseInt(nodeId);
                    for (int i = 0; i < fingerTable.length; i++) {


                        /**
                         * successor formula = succ(serverId+2^(i-1))
                         *
                         * successor is a possible node responsible for the values between
                         * the current and the successor.
                         *
                         * serverId equals to this node position in the circle
                         */
                        int succ = (int) (this.getNodeId() + Math.pow(2, (i - 1)));
                        /**
                         * if successor number is bigger than the circle size (max number of nodes)
                         * it starts counting from the beginning
                         * by removing this node position (serverId) from formula
                         */
                        if (succ > Math.pow(2, MAX_FINGER_TABLE_SIZE)) {
                            succ = (int) (Math.pow(2, (i - 1)));
                        }
                        /**
                         * if the successor is smaller than the value of the node we are readingee
                         * from the config file this means that the node we are reading might be
                         * responsible for the keys in between.
                         * If there isn't another node responsible
                         * for this interval or the node we are reading has a smaller value
                         * than the node that used to be responsible for this interval,
                         * than the node we are reading is now the node responsible
                         */
                        if (succ < id) {
                            if (fingerTable[i] == null) {
                                fingerTable[i] = new Node(nodeIp, nodePort);
                            } else if (id < fingerTable[i].getNodeId()) {
                                fingerTable[i] = new Node(nodeIp, nodePort);
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

    public void setPredecessor(Node node){
        this.predecessor=node;
    }

    /**
     * Regists user
     *
     * @param email    user email
     * @param password user password
     */
    public void addUser(String email, String password){

        System.out.println("Sign up with  " + email);

        BigInteger user_email = createHash(email);

        if(users.containsKey(user_email)){
            System.out.println("Email already exists. Try to sign in instead of sign up...");
        }
        else{
           users.put(user_email,new User(email,new BigInteger(password)));
           System.out.println("Signed up with success!");
       }
    }

    /**
     * Authenticates user already registered
     *
     * @param email    user email
     * @param password user password
     * @return true if user authentication went well, false if don't
     */
    public boolean loginUser(String email, String password) {

        System.out.println("Sign in with " + email);

        BigInteger user_email = createHash(email);

        if (users.get(user_email) == null) {
            System.out.println("Try to create an account. Your email was not found on the database...");
            return false;
        }

        if (!users.get(user_email).equals(createHash(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            return false;
        }

        System.out.println("Logged in with success!");

        return true;
    }
}

