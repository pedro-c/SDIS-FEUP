package Server;

import Chat.Chat;
import Messages.Message;
import Messages.MessageHandler;
import Utilities.Constants;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

public class Server extends Node implements Serializable {

    /**
     * Key is the user id (hash from e-mail) and value is the 256-bit hashed user password
     */
    private Hashtable<BigInteger, User> users;
    private ConcurrentHashMap<BigInteger, ServerChat> chats;

    //Logged in users
    private ConcurrentHashMap<BigInteger, SSLSocket> loggedInUsers;

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
        super(args[0], args[1]);
        users = new Hashtable<>();

        System.out.println("Server ID: " + this.getNodeId());

        initFingerTable();
        printFingerTable();
        initServerSocket();
        if (args.length > 2) {
            Node knownNode = new Node(args[2], args[3]);
            joinNetwork(this, knownNode);
        }

        //creating directories
        String usersPath = DATA_DIRECTORY + "/" + nodeId + "/" + USER_DIRECTORY;
        String chatsPath = DATA_DIRECTORY + "/" + nodeId + "/" + CHAT_DIRECTORY;

        createDir(DATA_DIRECTORY);
        createDir(DATA_DIRECTORY + "/" + Integer.toString(nodeId));
        createDir(usersPath);
        createDir(chatsPath);

        users = new Hashtable<>();

        chats = new ConcurrentHashMap<BigInteger, ServerChat>();
        loggedInUsers = new ConcurrentHashMap<BigInteger, SSLSocket>();
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
                SSLSocket socket = (SSLSocket) sslServerSocket.accept();
                sslServerSocket.setNeedClientAuth(true);
                ConnectionHandler handler = new ConnectionHandler(socket, this);
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
                if (node.getNodeId() > key) {
                    successor = node;
                    break;
                }
            } else {
                if (node.getNodeId() < previousId) {
                    if (key < node.getNodeId()) {
                        successor = node;
                    }
                }
            }


        }
        System.out.println("Successor of " + key + " : " + successor.getNodeId());
        return successor;
    }

    public boolean isResponsibleFor(BigInteger resquestId) {

        int tempId = Math.abs(resquestId.intValue());

        Node n = serverLookUp(tempId);

        if (n.getNodeId() == this.getNodeId())
            return true;


        return false;
    }


    public void redirect(Message request) {

        int tempId = Math.abs(request.getSenderId().intValue());

        Node n = serverLookUp(tempId);

        MessageHandler redirect = new MessageHandler(request, n.getNodeIp(), n.getNodePort(), this);

        redirect.connectToServer();
        redirect.sendMessage();

    }

    /**
     * This functions updates the server finger table with the new node info
     * @param newNode new node on the distributed hash table
     */
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
            } else if (newNode.getNodeId() < this.getNodeId()) {
                if (newNode.getNodeId() < node.getNodeId()) {
                    if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && MAX_NUMBER_OF_NODES - this.getNodeId() + node.getNodeId() > MAX_NUMBER_OF_NODES - this.getNodeId() + newNode.getNodeId()) {
                        if (node.getNodeId() < this.getNodeId() || node.getNodeId() == this.getNodeId()) {
                            fingerTable.set(i, newNode);
                            System.out.println("3");
                        }

                    } else if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && node.getNodeId() == this.getNodeId()) {
                        fingerTable.set(i, newNode);
                        System.out.println("4");
                    }
                }
            } else if (newNode.getNodeId() > this.getNodeId() && newNode.getNodeId() >= position && node.getNodeId() < position) {
                if (MAX_NUMBER_OF_NODES - this.getNodeId() + node.getNodeId() < MAX_NUMBER_OF_NODES - this.getNodeId() + newNode.getNodeId()) {
                    fingerTable.set(i, newNode);
                }
            }
        }
    }

    /**
     * Receives the successor finger table and updates is own finger table
     * @param successorFingerTable successor finger table
     */
    public void updateFingerTableFromSuccessor(ArrayList<Node> successorFingerTable) {

        System.out.println(successorFingerTable.size());
        for (int i = 0; i < successorFingerTable.size(); i++) {
            updateFingerTable(successorFingerTable.get(i));
        }

        printFingerTable();
    }

    /**
     * Function called when a new node message arrives to the server and forwards it to the correct server
     * @param info ip, port and id from the new server
     */
    public void newNode(String[] info) {
        int newNodeKey = Integer.parseInt(info[0]);
        String newNodeIp = info[1];
        String newNodePort = info[2];

        Node newNode = new Node(newNodeIp, newNodePort, newNodeKey);
        updateFingerTable(newNode);

        printFingerTable();

        Node successor = serverLookUp(newNodeKey);

        notifySuccessorOfItsPredecessor(successor, newNode);

        if (successor.getNodeId() == fingerTable.get(1).getNodeId()) {
            sendFingerTableToSuccessor();
        }

        if (successor.getNodeId() == predecessor.getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            System.out.println("Case1");
        } else if (newNode.getNodeId() > predecessor.getNodeId() && newNode.getNodeId() < this.getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            System.out.println("Case2");
        } else if (successor.getNodeId() == this.getNodeId() && fingerTable.get(MAX_FINGER_TABLE_SIZE).getNodeId() != this.getNodeId()) {
            joinNetwork(newNode, fingerTable.get(MAX_FINGER_TABLE_SIZE));
            System.out.println("Case3");
        }

        if (this.predecessor.getNodeId() == this.getNodeId())
            this.predecessor = newNode;
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

    public void sendFingerTableToPredecessor(Node newNode) {

        this.setPredecessor(newNode);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), fingerTable);

        MessageHandler handler = new MessageHandler(message, newNode.getNodeIp(), newNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    public void sendFingerTableToSuccessor() {

        Node successor = fingerTable.get(1);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), fingerTable);

        MessageHandler handler = new MessageHandler(message, successor.getNodeIp(), successor.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    public void notifySuccessorOfItsPredecessor(Node successor, Node newNode) {

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
     * Regists user
     *
     * @param email    user email
     * @param password user password
     */
    public Message addUser(String email, String password) {

        System.out.println("Sign up with  " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if (users.containsKey(user_email)) {
            System.out.println("Email already exists. Try to sign in instead of sign up...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), EMAIL_ALREADY_USED);
        } else {
            users.put(user_email, new User(email, new BigInteger(password)));
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
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
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), EMAIL_NOT_FOUND);
        } else if (!users.get(user_email).getPassword().equals(new BigInteger(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), WRONG_PASSWORD);
        } else {
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
     * Creates a new chat
     * New chat
     *
     * @return Message to be sent to the client
     */
    public Message createChat(Chat chat) {
        Message message = null;

        //TODO: FALAR COM OS SERVIDORES

        //This email is valid? Server knows?
        if (users.get(createHash(chat.getParticipant_email())) == null) {
            System.out.println("Invalid user Email.");
            message = new Message(Constants.CLIENT_ERROR, BigInteger.valueOf(nodeId), Constants.INVALID_USER_EMAIL);
        } else if (chats.get(chat.getIdChat()) != null) {
            System.out.println("Error creating chat");
            message = new Message(Constants.CLIENT_ERROR, BigInteger.valueOf(nodeId), Constants.ERROR_CREATING_CHAT);
        } else {

            ServerChat newChat = new ServerChat(chat.getIdChat(), chat.getParticipant_email());
            System.out.println("Created chat with success");
            newChat.addParticipant(users.get(createHash(chat.getParticipant_email())));
            System.out.println("Added participants with success");
            chats.put(newChat.getIdChat(), newChat);
            System.out.println("Stored chat with success");
            message = new Message(Constants.CLIENT_SUCCESS, BigInteger.valueOf(nodeId), chat);
        }

        return message;
    }

    /**
     * Saves client connection
     *
     * @param sslSocket
     * @param clientId
     */
    public void saveConnection(SSLSocket sslSocket, BigInteger clientId) {
        loggedInUsers.put(clientId, sslSocket);
    }

    public Node getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Node node) {

        if (this.predecessor.getNodeId() != node.getNodeId()) {
            updateFingerTable(node);
            this.predecessor = node;
            System.out.println("New predecessor: " + node.getNodeId());

        }
    }

    public void printFingerTable() {
        System.out.println("FINGERTABLE");
        System.out.println("-----------");
        System.out.println("Node ID: " + this.getNodeId());
        System.out.println("Predecessor: " + this.predecessor.getNodeId());
        System.out.println("-----------");
        System.out.println("FINGERtableSize: " + fingerTable.size());
        for (int i = 1; i < fingerTable.size(); i++) {
            System.out.println(i + "    " + fingerTable.get(i).getNodeId());
        }
        System.out.println("-----------");
    }

    public ArrayList<Node> getFingerTable() {
        return fingerTable;
    }

    public Message signOutUser(BigInteger userId){
        if(loggedInUsers.containsKey(userId)){
            loggedInUsers.remove(userId);
            System.out.println("Signed out user with id: " + userId);
        }

        return (new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId)));
    }
}

