package Server;

import Chat.Chat;
import Chat.ChatMessage;
import Messages.Message;
import Protocols.Connection;
import Protocols.DistributedHashTable;
import Protocols.ServerConnection;
import com.sun.org.apache.regexp.internal.RE;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.File;
import java.io.IOException;
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

        Message message = new Message(NEWNODE, BigInteger.valueOf(this.getNodeId()), RESPONSIBLE ,Integer.toString(newNode.getNodeId()), newNode.getNodeIp(), Integer.toString(newNode.getNodePort()));

        ServerConnection handler = new ServerConnection(knownNode.getNodeIp(), knownNode.getNodePort(), this);

        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(knownNode);
            joinNetwork(newNode, knownNode);
            return;
        }
        handler.sendMessage(message);
        handler.closeConnection();

    }

    /**
     * Handles a node failure, and alerts succesding node of such event
     * @param downServerId Id of the node that is down
     */
    public void handleNodeFailure(int downServerId, Message message){

        dht.removeNode(downServerId);

        if(message.getResponsible().equals(RESPONSIBLE)){
            beginNodeFailureProtocol();
            return;
        }

        Node downServerSuccessor = dht.nodeLookUp(downServerId+1);
        
        if(downServerSuccessor.getNodeId() == dht.getFingerTable().get(1).getNodeId()){
            message.setResponsible(RESPONSIBLE);
        }

        ServerConnection redirect = new ServerConnection(downServerSuccessor.getNodeIp(), downServerSuccessor.getNodePort(), this);
        try {
            redirect.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        redirect.sendMessage(message);
        redirect.closeConnection();

    }

    public void beginNodeFailureProtocol(){

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

        if (successor.getNodeId() == this.getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, previousPredecessor);
            sendInfoToPredecessor(newNode, users, ADD_USER);
            //sendInfoToPredecessor(newNode, backups, BACKUP_USER);
        } else if (newNode.getNodeId() > dht.getPredecessor().getNodeId()) {
            sendFingerTableToPredecessor(newNode);
            notifyNodeOfItsPredecessor(newNode, dht.getPredecessor());
            sendInfoToPredecessor(newNode, users, ADD_USER);
            //sendInfoToPredecessor(newNode, backups, BACKUP_USER);
        } else {
            joinNetwork(newNode, successor);
            System.out.println("Redirecting.");
        }
    }


    public void sendFingerTableToPredecessor(Node newNode) {

        dht.setPredecessor(newNode);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), RESPONSIBLE, dht.getFingerTable());

        ServerConnection handler = new ServerConnection(newNode.getNodeIp(), newNode.getNodePort(), this);

        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(newNode);
            sendFingerTableToPredecessor(newNode);
            return;
        }
        handler.sendMessage(message);
        handler.closeConnection();

    }

    public void sendFingerTableToSuccessor() {

        Node successor = dht.fingerTableNode(1);

        Message message = new Message(SUCCESSOR_FT, new BigInteger(Integer.toString(this.getNodeId())), RESPONSIBLE, dht.getFingerTable());

        ServerConnection handler = new ServerConnection(successor.getNodeIp(), successor.getNodePort(), this);

        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(successor);
            sendFingerTableToSuccessor();
            return;
        }
        handler.sendMessage(message);
        handler.closeConnection();
    }

    public void notifyNodeOfItsPredecessor(Node node, Node newNode) {

        Message message = new Message(PREDECESSOR, new BigInteger(Integer.toString(this.getNodeId())), RESPONSIBLE, newNode);

        ServerConnection handler = new ServerConnection(node.getNodeIp(), node.getNodePort(), this);

        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(node);
            notifyNodeOfItsPredecessor(node, newNode);
            return;
        }
        handler.sendMessage(message);
        handler.closeConnection();
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
            message = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), RESPONSIBLE, EMAIL_ALREADY_USED);
        } else {
            User newUser = new User(email, new BigInteger(password));
            users.put(user_email, newUser);
            message = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE);
            System.out.println("Account created with success!");
            sendInfoToBackup(new Message(BACKUP_USER, BigInteger.valueOf(nodeId), RESPONSIBLE, newUser));
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
        return new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, USER_ADDED);
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
            response = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), RESPONSIBLE, EMAIL_NOT_FOUND);
        } else if (!users.get(user_email).getPassword().equals(new BigInteger(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            response = new Message(CLIENT_ERROR, BigInteger.valueOf(nodeId), RESPONSIBLE, WRONG_PASSWORD);
        } else {
            System.out.println("Login with success!");
            response = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE);
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
    public Message createChat(ServerConnection connection, BigInteger senderId, Chat chat) {

        for (String participantEmail : chat.getParticipants()) {
            System.out.println(participantEmail);

            BigInteger participantHash = createHash(participantEmail);

            //if this server is responsible for this participant send client a message
            if(users.get(participantHash)!=null){
                users.get(participantHash).addChat(chat);

                if(chat.getCreatorEmail().equals(participantEmail)){
                    Message response = new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, chat.getIdChat().toString(),CREATED_CHAT_WITH_SUCCESS);
                    ServerConnection serverConnection = loggedInUsers.get(participantHash);
                    if(serverConnection != null)
                        serverConnection.sendMessage(response);
                }
                else inviteUserToChat(chat,participantHash);
            }
            else {
                Message message = new Message(CREATE_CHAT_BY_INVITATION, senderId, NOT_RESPONSIBLE, chat, participantHash);
                Runnable task = () -> { redirect(connection, message);};
                threadPool.submit(task);
            }
        }

        return new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, chat.getIdChat().toString(), SENT_INVITATIONS);

    }

    public Message inviteUserToChat(Chat chat, BigInteger clientId) {

        printLoggedInUsers();

        if(loggedInUsers.get(clientId) == null){
            System.out.println("Added to pending chats");
            if(users.get(clientId) != null)
                users.get(clientId).addPendingChat(chat);
        }
        else {
            users.get(clientId).addChat(chat);
            System.out.println("Sending invitation to logged in user");
            Message response = new Message(NEW_CHAT_INVITATION, BigInteger.valueOf(nodeId), RESPONSIBLE, chat, clientId);
            ServerConnection userConnection = loggedInUsers.get(clientId);
            userConnection.sendMessage(response);
        }

        return new Message(SERVER_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, SENT_INVITATIONS);
    }


    public Message sendMessage(ServerConnection connection, ChatMessage chatMessage, BigInteger clientId, BigInteger senderId) {

        Chat chat = users.get(clientId).getChat(chatMessage.getChatId());

        printLoggedInUsers();

        for (String participantEmail : chat.getParticipants()) {

            BigInteger participantHash = createHash(participantEmail);

            if(users.get(participantHash)!=null){

                if(!chatMessage.getUserId().toString().equals(participantHash.toString())){
                    sendMessageToUser(chatMessage, participantHash);
                }
            }
            else {
                Message message = new Message(NEW_MESSAGE_TO_PARTICIPANT, senderId, NOT_RESPONSIBLE, chatMessage, participantHash);
                Runnable task = () -> { redirect(connection, message);};
                threadPool.submit(task);
            }
        }

        return new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, chat.getIdChat().toString(), SENT_MESSAGE);

    }

    public Message sendMessageToUser(ChatMessage chatMessage, BigInteger clientId){

        if(loggedInUsers.get(clientId) == null){
            System.out.println("Added to pending messages");
            users.get(clientId).getChat(chatMessage.getChatId()).addPendingChatMessage(chatMessage);
        }
        else {
            System.out.println("Sending message to logged in user");
            users.get(clientId).getChat(chatMessage.getChatId()).addChatMessage(chatMessage);
            Message response = new Message(NEW_MESSAGE, BigInteger.valueOf(nodeId), RESPONSIBLE, chatMessage,clientId);
            ServerConnection userConnection = loggedInUsers.get(clientId);
            userConnection.sendMessage(response);
        }

        return new Message(SERVER_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, TEXT_MESSAGE);
    }


    /**
     * Returns chat to client
     * @param chatId
     * @param clientId
     * @return
     */
    public Message getChat(String chatId, BigInteger clientId){

        System.out.println(chatId);
        System.out.println(new BigInteger(chatId));

        Chat chat = users.get(clientId).getChat(new BigInteger(chatId));

        if(chat==null)
            System.out.println("Null Chat");

        Message message =  new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, chat);
        return message;
    }


    public Message getAllChats(BigInteger clientId){

        for(ConcurrentHashMap.Entry<BigInteger, Chat> entry : users.get(clientId).getChats().entrySet()) {
            Chat chat = entry.getValue();

            if(loggedInUsers.get(clientId) != null){
                Message message =  new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, chat);
                ServerConnection userConnection = loggedInUsers.get(clientId);
                userConnection.sendMessage(message);
            }
        }

        Message message =  new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, SENT_CHATS);
        return message;
    }

    public Message getAllPendingChats(BigInteger clientId){

        System.out.println(1);

        for(ConcurrentHashMap.Entry<BigInteger, Chat> entry : users.get(clientId).getPendingRequests().entrySet()) {
            Chat chat = entry.getValue();

            if(loggedInUsers.get(clientId) != null) {
                System.out.println(1);
                users.get(clientId).addChat(chat);
                System.out.println(2);
                users.get(clientId).deletePendingRequest(chat.getIdChat());
                System.out.println(3);
                Message response = new Message(NEW_CHAT_INVITATION, BigInteger.valueOf(nodeId), RESPONSIBLE, chat, clientId);
                System.out.println(4);
                ServerConnection userConnection = loggedInUsers.get(clientId);
                userConnection.sendMessage(response);
            }

        }

        Message message =  new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, SENT_PENDING_CHATS);
        return message;
    }

    /**
     * Saves client connection
     */
    public void saveConnection(ServerConnection connection, BigInteger clientId) {
        loggedInUsers.put(clientId, connection);
        printLoggedInUsers();
    }

    public void printLoggedInUsers() {

        System.out.println("");
        System.out.println("Logged in users");
        loggedInUsers.forEach((k, v) -> System.out.println("LOGGED IN : " + k));
        System.out.println("");
    }

    public void printUserChats(BigInteger client){
        users.get(client).chats.forEach((k, v) -> System.out.println("Chat : " + k));
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

        return (new Message(CLIENT_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE));
    }

    /**
     * Replicates info to his successor
     *
     * @param message message with all the info to be backed up
     */
    public void sendInfoToBackup(Message message) {
        Node successor = dht.getSuccessor();

        if (successor == null){
            System.out.println("Successor unavailable");
            return;
        }

        ServerConnection handler = new ServerConnection(successor.getNodeIp(), successor.getNodePort(), this);

        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(successor);
            sendInfoToBackup(message);
            return;
        }
        handler.sendMessage(message);
        try {
            handler.receiveMessage();
        } catch (IOException e) {
            System.out.println("Function sendInfoToBackup: Failed to receive message");
        } catch (ClassNotFoundException e) {
            System.out.println("Function sendInfoToBackup: Failed to receive message");
        }
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
                User user = (User) message.getObject();
                backups.put(user.getUserId(), user);
                System.out.println("Back up user from server " + message.getSenderId());
                response = new Message(SERVER_SUCCESS, BigInteger.valueOf(nodeId), RESPONSIBLE, BACKUP_USER_DONE);
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
        try {
            handler.connect();
        } catch (IOException e) {
            serverDown(node);
            sendInfoToPredecessor(node, container,type);
            return;
        }

        Runnable task = null;

        for (User user : predecessorUsers) {
            //type = ADD_USER or BACKUP_USER
            message = new Message(type, BigInteger.valueOf(nodeId), RESPONSIBLE, user);
            handler.sendMessage(message);
            try {
                handler.receiveMessage();
            } catch (IOException e) {
                System.out.println("Function sendInfoToPredecessor: Failed to receive message");
            } catch (ClassNotFoundException e) {
                System.out.println("Function sendInfoToPredecessor: Failed to receive message");
            }
        }
    }

    public boolean isToUseReceiver(String messageType) {

        switch (messageType) {
            case CREATE_CHAT_BY_INVITATION:
            case NEW_MESSAGE_TO_PARTICIPANT:
                return true;
        }

        return false;
    }


    public void isResponsible(ServerConnection connection, Message message) {
        String[] body ={""};
        if(message.getBody() != null)
            body = message.getBody().split(" ");

        System.out.println("REQUEST ID: " + Integer.remainderUnsigned(message.getSenderId().intValue(), 128));

        if(isToUseReceiver(message.getMessageType())){
            if (message.getResponsible().equals(NOT_RESPONSIBLE)){
                redirect(connection,message);
                return;
            }
        }
        else {
            if (message.getResponsible().equals(NOT_RESPONSIBLE)){
                redirect(connection,message);
                return;
            }
        }


        System.out.println("I'm the RESPONSIBLE server");

        Message response = null;

        switch (message.getMessageType()) {
            case SIGNIN:
                saveConnection(connection, message.getSenderId());
                response = loginUser(body[0],body[1]);
                break;
            case SIGNUP:
                saveConnection(connection, message.getSenderId());
                response = addUser(body[0],body[1]);
                break;
            case CREATE_CHAT:
                response = createChat(connection, message.getSenderId(), (Chat) message.getObject());
                break;
            case SIGNOUT:
                response = signOutUser(message.getSenderId());
                break;
            case GET_CHAT:
                response = getChat(body[0],message.getSenderId());
                break;
            case GET_ALL_CHATS:
               response = getAllChats(message.getSenderId());
                break;
            case GET_ALL_PENDING_CHATS:
                response = getAllPendingChats(message.getSenderId());
                break;
            case NEW_MESSAGE:
                response = sendMessage(connection, (ChatMessage) message.getObject(), message.getReceiver(), message.getSenderId());
                break;
            case CREATE_CHAT_BY_INVITATION:
                response = inviteUserToChat((Chat) message.getObject(), message.getReceiver());
                break;
            case NEW_MESSAGE_TO_PARTICIPANT:
                response = sendMessageToUser((ChatMessage) message.getObject(), message.getReceiver());
                break;
            default:
                break;
        }

        response.setInitialServerAddress(nodeIp);
        response.setInitialServerPort(nodePort);
        connection.sendMessage(response);
    }

    public void redirect(ServerConnection initialConnection, Message message) {

        int tempId;
        boolean foundResponsible = false;
        if(isToUseReceiver(message.getMessageType())){
            tempId = Integer.remainderUnsigned(message.getReceiver().intValue(),128);
            System.out.println("RECEIVER");
        }else{
            tempId = Integer.remainderUnsigned(message.getSenderId().intValue(),128);
            System.out.println("SENDER");
        }

        System.out.println("REDIRECTING ID: " + tempId);

        Node n = dht.nodeLookUp(tempId);

        if(n.getNodeId() == this.getNodeId()){
            System.out.println("Redirect to successor");
            n = dht.getFingerTable().get(1);
        }else if(n.getNodeId() == dht.getFingerTable().get(1).getNodeId()){
            System.out.println("Responsible for " + tempId + " is " + n.getNodeId());
            foundResponsible = true;
        }else{
            n = dht.nodeLookUp(n.getNodeId());
            if(n.getNodeId() == this.getNodeId()){
                System.out.println("Redirect to successor");
                n = dht.getFingerTable().get(1);
            }
            System.out.println("Jumping message to " + n.getNodeId());
        }

        ServerConnection redirect = new ServerConnection(n.getNodeIp(), n.getNodePort(), this);
        try {
            redirect.connect();
        } catch (IOException e) {
            serverDown(n);
            redirect(initialConnection,message);
            return;
        }
        if(foundResponsible){
            message.setResponsible(RESPONSIBLE);
        }else{
            message.setResponsible(NOT_RESPONSIBLE);
        }

        redirect.sendMessage(message);



        try {
            Message response = redirect.receiveMessage();
            initialConnection.sendMessage(response);

        } catch (IOException e) {
            System.out.println("Function redirect: Failed to receive message");
        } catch (ClassNotFoundException e) {
            System.out.println("Function redirect: Failed to receive message");
        }
        redirect.closeConnection();

    }

    public void serverDown(Node downNode){
        System.out.println("\n Node " + downNode.getNodeId() + " is down.");

        Node successor = dht.nodeLookUp(downNode.getNodeId()+1);


        Message message = new Message(SERVER_DOWN, new BigInteger(Integer.toString(this.getNodeId())), NOT_RESPONSIBLE, Integer.toString(downNode.getNodeId()));
        ServerConnection redirect = new ServerConnection(successor.getNodeIp(), successor.getNodePort(), this);

        try {
            redirect.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        redirect.sendMessage(message);
        dht.removeNode(downNode.getNodeId());

    }

    /**
     * Prints the code that comes from return messages (success or error)
     *
     * @param code
     */
    public void printReturnCodes(String code,BigInteger senderId){
        switch (code) {
            case BACKUP_USER_DONE:
                System.out.println("\nUser backed up by server " + senderId);
                break;
        }
    }

    public DistributedHashTable getDht() {
        return dht;
    }

    public ConcurrentHashMap<BigInteger, User> getBackups() {
        return backups;
    }

}

