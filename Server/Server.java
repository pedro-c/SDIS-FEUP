package Server;

import Chat.Chat;
import Messages.Message;
import Messages.MessageHandler;
import Protocols.DistributedHashTable;

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

    /**
     * Hash map to hold backups of files from this node predecessors
     * Key is the integer representing the userId and the value is the user Object
     */
    private ConcurrentHashMap<BigInteger, User> backups;

    private DistributedHashTable dht;
    /**
     * Logged in users
     */
    private ConcurrentHashMap<BigInteger, SSLSocket> loggedInUsers;
    transient private SSLServerSocket sslServerSocket;
    transient private SSLServerSocketFactory sslServerSocketFactory;
    transient private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);

    /**
     * @param args ServerId ServerPort KnownServerId KnownServer Port
     */
    public Server(String args[]) {
        super(args[0], Integer.parseInt(args[1]));
        users = new Hashtable<>();
        dht = new DistributedHashTable(this);

        System.out.println("Server ID: " + this.getNodeId());


        initServerSocket();
        if (args.length > 2) {
            Node knownNode = new Node(args[2], Integer.parseInt(args[3]));
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
        loggedInUsers = new ConcurrentHashMap<BigInteger, SSLSocket>();
        backups = new ConcurrentHashMap<BigInteger, User>();
    }

    /**
     * @param args [serverIp] [serverPort] [knownServerIp] [knownServerPort]
     */
    public static void main(String[] args) {
        Server server = null;
        server = new Server(args);
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
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(getNodePort());
            sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());

        } catch (IOException e) {
            System.out.println("Failed to create sslServerSocket");
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the network
     * Message: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
     */
    public void joinNetwork(Node newNode, Node knownNode) {

        Message message = new Message(NEWNODE, BigInteger.valueOf(this.getNodeId()), Integer.toString(newNode.getNodeId()), newNode.getNodeIp(), Integer.toString(newNode.getNodePort()));

        MessageHandler handler = new MessageHandler(message, knownNode.getNodeIp(), knownNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }


    public boolean isResponsibleFor(BigInteger resquestId) {

        int tempId = Math.abs(resquestId.intValue());

        Node n = dht.nodeLookUp(tempId);

        return n.getNodeId() == this.getNodeId();

    }


    public Node redirect(Message request) {

        int tempId = Math.abs(request.getSenderId().intValue());
        return dht.nodeLookUp(tempId);
    }

    /**
     * Function called when a new node message arrives to the server and forwards it to the correct server
     *
     * @param info ip, port and id from the new server
     */
    public void newNode(String[] info) {
        Node previousPredecessor = dht.getPredecessor();
        int newNodeKey = Integer.parseInt(info[0]);
        String newNodeIp = info[1];
        int newNodePort = Integer.parseInt(info[2]);

        Node newNode = new Node(newNodeIp, newNodePort, newNodeKey);
        dht.updateFingerTable(newNode);

        dht.printFingerTable();

        Node successor = dht.nodeLookUp(newNodeKey);

        if(successor.getNodeId() == this.getNodeId()){
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, previousPredecessor);
        }else if(newNode.getNodeId() > dht.getPredecessor().getNodeId()){
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, dht.getPredecessor());
        }else{
            joinNetwork(newNode, successor);
            System.out.println("Redirecting.");
        }
    }


    public void sendFingerTableToPredecessor(Node newNode) {

        dht.setPredecessor(newNode);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), dht.getFingerTable());

        MessageHandler handler = new MessageHandler(message, newNode.getNodeIp(), newNode.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    public void sendFingerTableToSuccessor() {

        Node successor = dht.fingerTableNode(1);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), dht.getFingerTable());

        MessageHandler handler = new MessageHandler(message, successor.getNodeIp(), successor.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

    }

    public void notifyNodeOfItsPredecessor(Node node, Node newNode) {

        Message message = new Message(PREDECESSOR, new BigInteger(Integer.toString(this.getNodeId())), newNode);

        MessageHandler handler = new MessageHandler(message, node.getNodeIp(), node.getNodePort(), this);

        handler.connectToServer();
        handler.sendMessage(message);

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
            User newUser = new User(email, new BigInteger(password));
            users.put(user_email, newUser);
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
            System.out.println("Signed up with success!");
            sendInfoToBackup(new Message(BACKUP_USER, BigInteger.valueOf(nodeId), email, password));
        }

        System.out.println("Sign up... Ready to response to client");

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

        ServerChat newChat = new ServerChat(chat.getIdChat(), chat.getCreatorEmail());
        users.get(createHash(chat.getCreatorEmail())).addChat(newChat);
        Message message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), newChat.getIdChat().toString());

        ServerChat chat1 = new ServerChat(chat.getIdChat(), chat.getParticipant_email());

        if (isResponsibleFor(createHash(chat.getParticipant_email()))) {
            users.get(createHash(chat.getParticipant_email())).addChat(chat1);
            //TODO: INVITE PARTICIPANT
            System.out.println("Added participant with success");
        } else {
            Node n = dht.nodeLookUp(createHash(chat.getParticipant_email()).intValue());
            Message message1 = new Message(INVITE_USER, BigInteger.valueOf(nodeId), chat1);
            MessageHandler redirect = new MessageHandler(message1, n.getNodeIp(), n.getNodePort(), this);
            System.out.println("Nada a ver comigo...XAU");
            threadPool.submit(redirect);
        }


        return message;
    }

    public Message createParticipantChat(ServerChat chat) {

        Message message = null;

        ServerChat newChat = new ServerChat(chat.getIdChat(), chat.getCreatorEmail());

        User user = users.get(createHash(chat.getCreatorEmail()));
        if (user != null) {
            user.addChat(newChat);
            printLoggedInUsers();
            System.out.println(user.getUserId());
            if (loggedInUsers.get(user.getUserId()) != null) {
                message = new Message(NEW_CHAT_INVITATION, BigInteger.valueOf(nodeId), newChat);
                SSLSocket socket = loggedInUsers.get(user.getUserId());
                //TODO: BLOCKING
                writeToSocket(socket, message);
                System.out.println(5);
            }
        } else {
            System.out.println("User not registry");
        }
        return message;
    }

    public void writeToSocket(SSLSocket sslSocket, Message message) {
        ObjectOutputStream outputStream = null;
        ObjectInputStream inputStream = null;

        try {
            outputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            inputStream = new ObjectInputStream(sslSocket.getInputStream());
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves client connection
     *
     * @param sslSocket
     * @param clientId
     */
    public void saveConnection(SSLSocket sslSocket, BigInteger clientId) {
        loggedInUsers.put(clientId, sslSocket);
        printLoggedInUsers();
    }

    public void printLoggedInUsers() {
        loggedInUsers.forEach((k, v) -> System.out.println("LOGGED IN : " + k));
    }

    /**
     * Function used to sign out users, this user is removed from the logged-in users arraylist
     * @param userId id of the user
     * @return message
     */
    public Message signOutUser(BigInteger userId) {
        if (loggedInUsers.containsKey(userId)) {
            loggedInUsers.remove(userId);
            System.out.println("Signed out user with id: " + userId);
        }

        return (new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId)));
    }

    /**
     * Replicates info to his successor
     * @param message message with all the info to be backed up
     */
    public void sendInfoToBackup(Message message){
        Node successor = dht.getSuccessor();

        if(successor == null)
            return;

        MessageHandler handler = new MessageHandler(message, successor.getNodeIp(),
                successor.getNodePort(), this);

        threadPool.submit(handler);
    }

    /**
     * Function used when a BACKUP request arrives to the server, basically depending on the request
     * this function add, update or delete the information
     * @param message message with all the information and the type of the request
     * @return message of success or error
     */
    public Message backupInfo(Message message) {
        Message response = null;
        String[] body;

        switch (message.getMessageType()) {
            case BACKUP_USER:
                body = message.getBody().split(" ");
                User user = new User(body[0], new BigInteger(body[1]));
                backups.put(user.getUserId(), user);
                response = new Message(SERVER_SUCCESS, BigInteger.valueOf(this.getNodeId()), USER_ADDED);
                break;
            default:
                break;
        }

        return response;
    }

    /**
     * Decides what to do depending on the situation
     * @param response response by a server or client of a message sent the server
     */
    public void verifyState(Message response){

        String body[] = response.getBody().split(" ");

        if(body.length == 0)
            return;

        switch (body[0]){
            case USER_ADDED:
                System.out.println("Node with id " + response.getSenderId() + " backed up user");
                break;
            default:
                break;
        }
    }

    public Hashtable<BigInteger, User> getUsers() {
        return users;
    }

    public void setUsers(Hashtable<BigInteger, User> users) {
        this.users = users;
    }

    public DistributedHashTable getDht() {
        return dht;
    }

    public void setDht(DistributedHashTable dht) {
        this.dht = dht;
    }


    public ConcurrentHashMap<BigInteger, SSLSocket> getLoggedInUsers() {
        return loggedInUsers;
    }

    public void setLoggedInUsers(ConcurrentHashMap<BigInteger, SSLSocket> loggedInUsers) {
        this.loggedInUsers = loggedInUsers;
    }
}

