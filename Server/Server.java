package Server;

import Messages.Message;
import Chat.Chat;
import Messages.MessageHandler;
import Utilities.Constants;

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

public class Server extends Node implements Serializable{

    /**
     * Key is the user id (hash from e-mail) and value is the 256-bit hashed user password
     */
    private Hashtable<BigInteger, User> users;

    private ArrayList<Node> serversInfo;
    /**
     * Key is an integer representing the m nodes and the value it's the server identifier
     * (32-bit integer hash from ip+port)
     */
    private ArrayList<Node> fingerTable = new ArrayList<Node>();
    transient private SSLServerSocket sslServerSocket;
    transient private SSLServerSocketFactory sslServerSocketFactory;
    transient private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private Node predecessor = this;

    /**
     * @param args ServerId ServerPort KnownServerId KnownServer Port
     */
    public Server(String args[]) {
        super(Integer.parseInt(args[0]), args[1], args[2]);
        users = new Hashtable<>();
        serversInfo = new ArrayList<Node>();

        System.out.println("Server ID: " + this.getNodeId());

        initFingerTable();
        printFingerTable();
        initServerSocket();
        if (args.length > 3) {
            Node knownNode = new Node(Integer.parseInt(args[3]), args[4], args[5]);
            joinNetwork(this,knownNode);
        }
    }

    /**
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
        for (int i = 0; i <= MAX_FINGER_TABLE_SIZE; i++) {
            fingerTable.add(this);
        }
    }

    /**
     * Sends a message to the network
     * Message: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
     */
    public void joinNetwork(Node newNode, Node knownNode) {

        Message message = new Message(NEWNODE, BigInteger.valueOf(this.getNodeId()), Integer.toString(newNode.getNodeId()), newNode.getNodeIp(), newNode.getNodePort());

        MessageHandler handler = new MessageHandler(message, knownNode.getNodeIp(), knownNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public Node serverLookUp(int key) {

        long distance, position;
        Node successor = this;
        long previousId = this.getNodeId();
        for (int i = 1; i < fingerTable.size(); i++) {
            Node node = fingerTable.get(i);

            distance = (long) Math.pow(2, (double) i - 1);
            position = this.getNodeId() + distance;
            if (key > this.getNodeId()) {
                if (node.getNodeId() > key){
                    successor = node;
                    break;
                }
            }else{
                if(node.getNodeId() < previousId){
                    if(key<node.getNodeId()){
                        successor=node;
                    }
                }
            }


        }
        System.out.println("Successor of " + key + " : " + successor.getNodeId());
        return successor;
    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     * and returns its predecessor
     *
     * @param key 32-bit identifier
     * @return Predecessor node
     */
    public Node predecessorLookUp(int key) {
        Node id = this;
        for (int i = 1; i < fingerTable.size(); i++) {
            id = fingerTable.get(i);
            if (id.getNodeId() > key && i > 1) {
                return fingerTable.get(i - 1);
            } else if (id.getNodeId() > key && i > 1) {
                return this;
            }
        }
        return id;
    }

    public void updateFingerTable(Node newNode) {

        long position;
        long distance;

        for (int i = 1; i < fingerTable.size(); i++) {
            Node node = fingerTable.get(i);

            distance = (long) Math.pow(2, (double) i - 1);
            position = this.getNodeId() + distance;
            if (node.getNodeId() == this.getNodeId() && newNode.getNodeId() >= position) {
                fingerTable.set(i, newNode);
                System.out.println("1");
            } else if (newNode.getNodeId() >= position && newNode.getNodeId() < node.getNodeId()) {
                fingerTable.set(i, newNode);
                System.out.println("2");
            }else if (newNode.getNodeId() < this.getNodeId()) {
                if (newNode.getNodeId() < node.getNodeId()) {
                    if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && MAX_NUMBER_OF_NODES - this.getNodeId() + node.getNodeId() > MAX_NUMBER_OF_NODES - this.getNodeId() + newNode.getNodeId()) {
                        if(node.getNodeId() < this.getNodeId() || node.getNodeId() == this.getNodeId()){
                            fingerTable.set(i, newNode);
                            System.out.println("3");
                        }

                    } else if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && node.getNodeId() == this.getNodeId()) {
                        fingerTable.set(i, newNode);
                        System.out.println("4");
                    }
                }
            }else if(newNode.getNodeId() > this.getNodeId() && newNode.getNodeId() >= position && node.getNodeId() < position){
                if(MAX_NUMBER_OF_NODES - this.getNodeId() + node.getNodeId() < MAX_NUMBER_OF_NODES - this.getNodeId() + newNode.getNodeId()) {
                    fingerTable.set(i, newNode);
                }
            }
        }
    }

    public void joinedNetwork(ArrayList<Node> successorFingerTable){

        System.out.println("dasdjasldas");
        System.out.println(successorFingerTable.size());
        for(int i = 1; i<successorFingerTable.size(); i++){
            updateFingerTable(successorFingerTable.get(i));
        }

        printFingerTable();
    }

    public void newNode(String[] info) {
        int newNodeKey = Integer.parseInt(info[0]);
        String newNodeIp = info[1];
        String newNodePort = info[2];

        Node newNode = new Node(newNodeIp, newNodePort, newNodeKey);
        updateFingerTable(newNode);

        printFingerTable();

        Node successor = serverLookUp(newNodeKey);

        notifySuccessorOfItsPredecessor(newNode);

        if(successor == predecessor){
            sendWelcomeMessage(newNode);
            System.out.println("Case1");
        }else if(newNode.getNodeId() > predecessor.getNodeId() && newNode.getNodeId() < this.getNodeId()){
            sendWelcomeMessage(newNode);
            System.out.println("Case2");
        }else if(successor == this){
            joinNetwork(newNode, fingerTable.get(MAX_FINGER_TABLE_SIZE));
            System.out.println("Case3");
        }else{
            joinNetwork(newNode,successor);
            System.out.println("Case4");
        }

        if(this.predecessor == this)
            this.predecessor=newNode;
    }

    /**
     * Gets the position of the new node em relation to the peer
     *
     * @param key key of the new node
     * @return 0 if the new node it's before, 1 if it's after
     */
    public int getNewNodePosition(int key) {

        if (getNodeId() > key)
            return BEFORE;

        return AFTER;
    }

    public void sendWelcomeMessage(Node newNode){

        Message message = new Message(WELCOME_TO_DHT, new BigInteger(Integer.toString(this.getNodeId())), fingerTable);

        MessageHandler handler = new MessageHandler(message, newNode.getNodeIp(), newNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    public void notifySuccessorOfItsPredecessor(Node newNode){

        Node successor = fingerTable.get(1);

        Message message = new Message(PREDECESSOR, new BigInteger(Integer.toString(this.getNodeId())), newNode);

        MessageHandler handler = new MessageHandler(message, successor.getNodeIp(), successor.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

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
                    for (int i = 0; i < fingerTable.size(); i++) {


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
                            if (fingerTable.get(i) == null) {
                                //fingerTable.add(new Node(nodeIp, nodePort));
                            } else if (id < fingerTable.get(i).getNodeId()) {
                                //fingerTable.add(new Node(nodeIp, nodePort));
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
     * Regists user
     *
     * @param email    user email
     * @param password user password
     */
    public Message addUser(String email, String password){

        System.out.println("Sign up with  " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if(users.containsKey(user_email)){
            System.out.println("Email already exists. Try to sign in instead of sign up...");
            message = new Message(Constants.CLIENT_ERROR, BigInteger.valueOf(nodeId), Constants.EMAIL_ALREADY_USED);
        }
        else{
            users.put(user_email,new User(email,new BigInteger(password)));
            message = new Message(Constants.CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
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
            message = new Message(Constants.CLIENT_ERROR, BigInteger.valueOf(nodeId),Constants.EMAIL_NOT_FOUND);
        }
        else if (!users.get(user_email).getPassword().equals(new BigInteger(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            message = new Message(Constants.CLIENT_ERROR, BigInteger.valueOf(nodeId),Constants.WRONG_PASSWORD);
        }
        else {
            System.out.println("Logged in with success!");
            message = new Message(Constants.CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
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

                if(!serversInfo.contains(node) && nodeId != node.getNodeId()){
                    System.out.println("Read server with ip: " + infos[0] + " and port " + infos[1]);
                    serversInfo.add(node);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new chat
     * New chat
     * @return Message to be sent to the client
     */
    public Message createChat(Chat chat){
        Message message = null;

        System.out.println(chat.getChatName());

        return message;

    }

    public Node getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Node node) {
        updateFingerTable(node);
        this.predecessor = node;
        System.out.println("New predecessor: " + node.getNodeId());
    }

    public void printFingerTable() {
        System.out.println("FINGERTABLE");
        System.out.println("-----------");
        System.out.println("Node ID: " + this.getNodeId());
        System.out.println("-----------");
        System.out.println("FINGERtableSize: " + fingerTable.size());
        for (int i = 1; i < fingerTable.size(); i++) {
            System.out.println(i + "    " + fingerTable.get(i).getNodeId());
        }
        System.out.println("-----------");
    }
}

