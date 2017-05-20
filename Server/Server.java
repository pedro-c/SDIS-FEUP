package Server;

import Chat.Chat;
import Messages.Message;
import Protocols.ClientConnection;
import Protocols.Connection;
import Protocols.DistributedHashTable;
import Protocols.ServerConnection;
import Utilities.Utilities;
import com.sun.org.apache.bcel.internal.generic.NEW;

import javax.lang.model.element.Name;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

public class Server extends Node implements Serializable {

    /**
     * Key is the user id (hash from e-mail) and value is the 256-bit hashed user password
     */
    private ConcurrentHashMap<BigInteger, User> users;

    /**
     * Hash map to hold backups of files from this node predecessors
     * Key is the integer representing the userId and the value is the user Object
     */
    private ConcurrentHashMap<BigInteger, User> backups;

    private DistributedHashTable dht;
    /**
     * Logged in users
     */
    private ConcurrentHashMap<BigInteger, ServerConnection> loggedInUsers;
    transient private SSLServerSocket sslServerSocket;
    transient private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);

    /**
     * @param args ServerId ServerPort KnownServerId KnownServer Port
     */
    public Server(String args[]) {
        super(args[0], Integer.parseInt(args[1]));
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

        users = new ConcurrentHashMap<>();
        loggedInUsers = new ConcurrentHashMap<>();
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

                ServerConnection connection = new ServerConnection(socket, this);
                threadPool.submit(connection);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initiates the server socket for incoming requests
     */
    public void initServerSocket() {
        SSLServerSocketFactory sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(getNodePort());
            sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to create sslServerSocket");
        }
    }

    /**
     * Sends a message to the network
     * Message: [NEWNODE] [SenderID] [NodeID] [NodeIp] [NodePort]
     */
    public void joinNetwork(Node newNode, Node knownNode) {

        Message message = new Message(NEWNODE, BigInteger.valueOf(this.getNodeId()), Integer.toString(newNode.getNodeId()), newNode.getNodeIp(), Integer.toString(newNode.getNodePort()));

        ServerConnection handler = new ServerConnection(knownNode.getNodeIp(), knownNode.getNodePort(), this);

        handler.connect();
        handler.sendMessage(message);

    }

    /**
     * Verifies if this server is responsible for a given client
     * @param clientId client id
     * @return
     */
    public boolean isResponsibleFor(BigInteger clientId) {

        int tempId = Math.abs(clientId.intValue());

        Node n = dht.nodeLookUp(tempId);

        return n.getNodeId() == this.getNodeId();
    }

    /**
     * Function called when a new node message arrives to the server and forwards it to the correct server
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

        sendFingerTableToSuccessor();
        sendFingerTableToPredecessor(dht.getPredecessor());

        //TODO VER O BACKUP DE NOVO
        if (successor.getNodeId() == this.getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, previousPredecessor);
            /*sendInfoToPredecessor(newNode, users, ADD_USER);
            sendInfoToPredecessor(newNode, backups, BACKUP_USER);*/
        } else if (newNode.getNodeId() > dht.getPredecessor().getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, dht.getPredecessor());
            /*sendInfoToPredecessor(newNode, users, ADD_USER);
            sendInfoToPredecessor(newNode, backups, BACKUP_USER);*/
        } else {
            joinNetwork(newNode, successor);
            System.out.println("Redirecting.");
        }
    }


    public void sendFingerTableToPredecessor(Node newNode) {

        dht.setPredecessor(newNode);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), dht.getFingerTable());

        ServerConnection handler = new ServerConnection(newNode.getNodeIp(), newNode.getNodePort(), this);

        handler.connect();
        handler.sendMessage(message);

    }

    public void sendFingerTableToSuccessor() {

        Node successor = dht.fingerTableNode(1);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), dht.getFingerTable());

        ServerConnection handler = new ServerConnection(successor.getNodeIp(), successor.getNodePort(), this);

        handler.connect();
        handler.sendMessage(message);

    }

    public void notifyNodeOfItsPredecessor(Node node, Node newNode) {

        Message message = new Message(PREDECESSOR, new BigInteger(Integer.toString(this.getNodeId())), newNode);

        ServerConnection handler = new ServerConnection(node.getNodeIp(), node.getNodePort(), this);

        handler.connect();
        handler.sendMessage(message);

    }

    /**
     * Regists user
     *
     * @param email    user email
     * @param password user password
     * @return response message
     */
    public Message addUser(String email, String password) {

        System.out.println("\nCreating account to user with email:  " + email);

        BigInteger user_email = createHash(email);

        Message message;

        if (users.containsKey(user_email)) {
            System.out.println("Email already exists. Try to sign in instead of sign up...");
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), EMAIL_ALREADY_USED);
        } else {
            User newUser = new User(email, new BigInteger(password));
            users.put(user_email, newUser);
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
            System.out.println("Account created with success!");
            //sendInfoToBackup(new Message(BACKUP_USER, BigInteger.valueOf(nodeId), email, password));
        }

        return message;
    }

    /**
     * Regists user
     *
     * @param newUser
     * @return response message
     */
    public Message addUser(User newUser) {
        System.out.println("Recebendo user do meu sucessor");

        BigInteger userId = createHash(newUser.getEmail());

        users.put(userId, newUser);
        return new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), USER_ADDED);
    }

    /**
     * Authenticates user already registered
     *
     * @param email  message
     * @param password message
     * @return true if user authentication went well, false if don't
     */
    public Message loginUser(String email, String password) {

        System.out.println("\nUser with email " + email + " trying to login!");
        BigInteger user_email = createHash(email);
        Message response;

        if (users.get(user_email) == null) {
            System.out.println("Try to create an account. Your email was not found on the database...");
            response = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), EMAIL_NOT_FOUND);
        } else if (!users.get(user_email).getPassword().equals(new BigInteger(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            response = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), WRONG_PASSWORD);
        } else {
            System.out.println("Login with success!");
            response = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId));
        }

        return response;
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
    public Message createChat(ServerConnection connection, Chat chat) {


        for (String participant_email : chat.getParticipants()) {


            //if this server is responsible for this participant send client a message
            if(users.get(createHash(participant_email))!=null){
                users.get(createHash(participant_email)).addChat(chat);

                //TODO: VERFICIAR SE ESTA LOGGADO?
                if(chat.getCreatorEmail()==participant_email){
                    Message response = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId),chat.getIdChat().toString(),CREATED_CHAT_WITH_SUCCESS);
                    ServerConnection serverConnection = loggedInUsers.get(createHash(participant_email));
                    serverConnection.sendMessage(response);
                }
                else if((loggedInUsers.get(createHash(participant_email))!=null)){ //if client is logged in
                    Message response = new Message(NEW_CHAT_INVITATION, BigInteger.valueOf(nodeId),chat.getIdChat().toString());
                    ServerConnection serverConnection = loggedInUsers.get(createHash(participant_email));
                    serverConnection.sendMessage(response);
                }
                else{
                    //If client is not logged in, server adds chat to pending requests
                    users.get(createHash(participant_email)).addPendingChat(chat);
                }
            }
            else if (users.get(createHash(chat.getCreatorEmail())) != null) {
                Message message = new Message(CREATE_CHAT, BigInteger.valueOf(nodeId), chat);
                Runnable task = () -> { redirect(connection, message);};
                threadPool.submit(task);
            }
        }

        return new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), chat.getIdChat().toString(), SENT_INVITATIONS);

    }

    /**
     * Saves client connection
     */
    public void saveConnection(ServerConnection connection, BigInteger clientId) {
        loggedInUsers.put(clientId, connection);
        //printLoggedInUsers();
    }

    public void printLoggedInUsers() {

        System.out.println("");
        System.out.println("Logged in users");
        loggedInUsers.forEach((k, v) -> System.out.println("LOGGED IN : " + k));
        System.out.println("");
    }

    /**
     * Function used to sign out users, this user is removed from the logged-in users arrayList
     * @param userId id of the user
     * @return message
     */
    public Message signOutUser(BigInteger userId) {
        if (loggedInUsers.containsKey(userId)) {
            loggedInUsers.remove(userId);
            System.out.println("\nSigned out user with id: " + userId);
        }

        return (new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId)));
    }

    /**
     * Replicates info to his successor
     *
     * @param message message with all the info to be backed up
     */
    public void sendInfoToBackup(Message message) {
        Node successor = dht.getSuccessor();

        if (successor == null)
            return;

        ServerConnection handler = new ServerConnection(successor.getNodeIp(), successor.getNodePort(), this);

        handler.connect();
        handler.sendMessage(message);
        handler.receiveMessage();
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
                System.out.println("Back up user from server " + message.getSenderId());
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
     *
     * @param response response by a server or client of a message sent the server
     */
    public void verifyState(Message response) {

        String body[] = response.getBody().split(" ");

        if (body.length == 0)
            return;

        switch (body[0]) {
            case USER_ADDED:
                System.out.println("Node with id " + response.getSenderId() + " backed up user");
                break;
            default:
                break;
        }
    }

    /**
     * Gets the respective users of a new server from a given container(users,backups) of the server
     *
     * @param node      New node/server
     * @param container server user containers, users and backups
     * @return a container of users
     */
    public Queue<User> getUsersOfANewServer(Node node, ConcurrentHashMap<BigInteger, User> container) {

        Queue<User> newServerUsers = new LinkedList<User>();

        BigInteger newNodeId = BigInteger.valueOf(node.getNodeId());

        container.forEach((userId, user) -> {

            // -1 userId is greater
            // 0 the values are equal
            // 1 newNodeId is greater
            int result = newNodeId.compareTo(userId);

            if (result == -1 || result == 0) {
                newServerUsers.add(user);
                users.remove(userId, user);
            }
        });

        return newServerUsers;
    }

    public void sendInfoToPredecessor(Node node, ConcurrentHashMap<BigInteger, User> container, String type) {

        Queue<User> predecessorUsers = getUsersOfANewServer(node, container);

        System.out.println("Enviando info para o predecessor");

        Message message = null;
        ServerConnection handler = new ServerConnection(node.getNodeIp(), node.getNodePort(), this);
        handler.connect();

        for (User user : predecessorUsers) {
            //type = ADD_USER or BACKUP_USER
            message = new Message(type, BigInteger.valueOf(nodeId), user);
            handler.sendMessage(message);
            handler.receiveMessage();
        }
    }

    public void isResponsible(ServerConnection connection, Message message) {
        String[] body ={""};
        if(message.getBody() != null)
            body = message.getBody().split(" ");

        System.out.println("REQUEST ID: " + Integer.remainderUnsigned(message.getSenderId().intValue(), 128));

        if (!isResponsibleFor(message.getSenderId())){
            redirect(connection,message);
            return;
        }

        System.out.println("I'm the RESPONSIBLE server");

        saveConnection(connection, message.getSenderId());
        Message response = null;

        switch (message.getMessageType()) {
            case SIGNIN:
                response = loginUser(body[0],body[1]);
                break;
            case SIGNUP:
                response = addUser(body[0],body[1]);
                break;
            case CREATE_CHAT:
                response = createChat(connection, (Chat) message.getObject());
                break;
            case SIGNOUT:
                response = signOutUser(message.getSenderId());
            default:
                break;
        }

        response.setInitialServerAddress(nodeIp);
        response.setInitialServerPort(nodePort);
        connection.sendMessage(response);
    }

    public void redirect(ServerConnection initialConnection, Message message) {
        System.out.println("REDIRECTING ID: " + message.getSenderId().intValue());

        int tempId = Math.abs(message.getSenderId().intValue());
        Node n = dht.nodeLookUp(tempId);

        ServerConnection redirect = new ServerConnection(n.getNodeIp(), n.getNodePort(), this);
        redirect.connect();
        redirect.sendMessage(message);
        initialConnection.sendMessage(redirect.receiveMessage());
    }

    public DistributedHashTable getDht() {
        return dht;
    }

    public ConcurrentHashMap<BigInteger, User> getBackups() {
        return backups;
    }
}

