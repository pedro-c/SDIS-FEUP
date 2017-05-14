package Server;

import Messages.Message;
import Messages.MessageHandler;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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
     * ArrayList with a list of servers (ip, port and id)
     */
    private ArrayList<Node> serversList;

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

        //creating directories
        String usersPath = DATA_DIRECTORY + "/" + nodeId + "/" + USER_DIRECTORY;
        String chatsPath = DATA_DIRECTORY + "/" + nodeId + "/" + CHAT_DIRECTORY;

        createDir(DATA_DIRECTORY);
        createDir(DATA_DIRECTORY + "/" + Integer.toString(nodeId));
        createDir(usersPath);
        createDir(chatsPath);

        users = new Hashtable<>();

        serversList = new ArrayList<Node>();

        loadServersInfo();
    }

    /**
     *
     * @param args [serverIp] [serverPort]
     */
    public static void main(String[] args) {

        if(args.length != 2){
            throw new IllegalArgumentException("\nUsage : java Server.Server <serverIp> <serverPort>");
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

        Message message = new Message(NEWNODE, BigInteger.valueOf(this.getNodeId()), Integer.toString(predecessor.getNodeId()), predecessor.getNodeIp(), predecessor.getNodePort());

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
    public Message addUserByClient(String email, String password){

        System.out.println("Sign up with  " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if(users.containsKey(user_email)){
            System.out.println("Email already exists. Try to sign in instead of sign up...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), EMAIL_ALREADY_USED);
        }
        else{
            users.put(user_email,new User(email,new BigInteger(password)));
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
            System.out.println("Signed up with success!");

            //Lets send the user info to the others servers
            Message serverMessage;
            MessageHandler serverHandler;
            for(Node server: serversList){
                serverMessage = new Message(ADD_USER, BigInteger.valueOf(nodeId), email, password);
                serverHandler= new MessageHandler(serverMessage, server.getNodeIp(), server.getNodePort(), this);
                threadPool.submit(serverHandler);
            }
        }

        return message;
    }

    /**
     * Registers a user
     * @param email
     * @param password
     * @return return message (success or error)
     */
    public Message addUserByServer(String email, String password){
        System.out.println("Sign up with  " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if(users.containsKey(user_email)){
            System.out.println("Email already exists. Try to sign in instead of sign up...");
            message = new Message(SERVER_ERROR, BigInteger.valueOf(nodeId), EMAIL_ALREADY_USED);
        }
        else{
            users.put(user_email,new User(email,new BigInteger(password)));
            message = new Message(SERVER_SUCCESS, BigInteger.valueOf(nodeId));
            System.out.println("Signed up with success!");
        }

        return message;
    }

    /**
     * Authenticates user already registered
     *
     * @param email    user email
     * @param password user password
     * @return true if user authentication went well, false if don't
     */
    public Message loginUser(String email, String password) {

        System.out.println("Sign in with " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if (users.get(user_email) == null) {
            System.out.println("Try to create an account. Your email was not found on the database...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId),EMAIL_NOT_FOUND);
        }
        else if (!users.get(user_email).getPassword().equals(new BigInteger(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId),WRONG_PASSWORD);
        }
        else {
            System.out.println("Logged in with success!");
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
        }

        return message;
    }

    /**
     * Create a directory
     *
     * @param path path of the directory to be created
     */
    private void createDir(String path) {

        File file = new File(path);

        if (file.mkdir()) {
            System.out.println("Directory: " + path + " created");
        }
    }

    /**
     * Loads all servers from a file
     */
    private void loadServersInfo(){
        try {
            List<String> lines = Files.readAllLines(Paths.get(SERVERS_INFO));
            for(String line : lines){
                String infos[] = line.split(" ");
                Node node = new Node(infos[0],infos[1]);

                if(!serversList.contains(node) && nodeId != node.getNodeId()){
                    System.out.println("Read server with ip: " + infos[0] + " and port " + infos[1]);
                    serversList.add(node);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeServerFromList(String ip,int port){
        Node node = new Node(ip,Integer.toString(port));

        if(serversList.contains(node)){
            if(serversList.remove(node)){
                System.out.println("\nRemoved server with ip: " + node.getNodeIp() +
                        " and port " + node.getNodePort() + " from servers list\n");
            }

        }
    }
}

