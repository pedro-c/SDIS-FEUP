package Client;

import Chat.Chat;
import Chat.ChatMessage;
import Messages.Message;
import Protocols.ClientConnection;
import Server.User;
import Utilities.Constants;

import java.io.Console;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Client.Client.Task.*;
import static Utilities.Constants.*;
import static Utilities.Utilities.*;

public class Client extends User{

    private Scanner scannerIn;
    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private ClientConnection connection;

    private Task actualState;
    private ConcurrentHashMap<BigInteger, Chat> chats;
    private int currentChat = 0;

    private int serverPort;
    private String serverIp;

    private int recoverServerPort;
    private String recoverServerIp;

    /**
     * Client
     */
    public Client(String serverIp, int serverPort) {
        super(null,null);
        this.serverPort = serverPort;
        this.serverIp = serverIp;
        this.actualState = HOLDING;
        scannerIn = new Scanner(System.in);
        connection = new ClientConnection(serverIp, serverPort, this);
        try {
            connection.connect();
        } catch (IOException e) {
            //Iniciar o protocolo
            System.out.println("\nError connecting");
        }
        chats = new ConcurrentHashMap<BigInteger, Chat>();
        //Listen
        threadPool.submit(connection);
    }

    public Client(String serverIp, int serverPort, String recoverServerIp, int recoverServerPort) {
        this(serverIp,serverPort);

        this.recoverServerIp = recoverServerIp;
        this.recoverServerPort = recoverServerPort;
    }

    /**
     * Main
     * @param args initial arguments
     */
    public static void main(String[] args) {

        if (args.length != 2 || args.length != 4) {
            throw new IllegalArgumentException("\nUsage : java Client.Client <serverIp> <serverPort> (<recoverServerIp> <recoverServerPort>)");
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        if (serverPort < 0 && serverPort > 65535) {
            throw new IllegalArgumentException("\nThe port needs to be between 0 and 65535");
        }

        Client client;

        if(args.length == 4) {
            String recoverServerIp = args[2];
            int recoverServerPort = Integer.parseInt(args[3]);

            client = new Client(serverIp, serverPort, recoverServerIp, recoverServerPort);
        }
        else {
            client = new Client(serverIp, serverPort);
        }

        client.mainMenu();
    }

    /**
     * Prints main menu
     */
    public void mainMenu() {
        String menu = "\n Menu " + "\n 1. Sign in" + "\n 2. Sign up" + "\n 3. Exit" + "\n";
        System.out.println(menu);
        int option = scannerIn.nextInt();
        switch (option) {
            case 1:
                signInUser();
                break;
            case 2:
                signUpUser();
                break;
            case 3:
            default:
                mainMenu();
        }
    }

    /**
     * Logged in user menu
     */
    public void signInMenu() {
        actualState = Task.HOLDING;
        currentChat = Constants.NO_CHAT_OPPEN;
        String menu = "\n Menu " + "\n 1. Create a new Chat" + "\n 2. Open Chat" + "\n 3. Send Files" + "\n 4. Sign Out" + "\n";
        System.out.println(menu);


        askForClientChats();
        //askForPendingChats();

        int option = scannerIn.nextInt();
        switch (option) {
            case 1:
                actualState = CREATING_CHAT;
                createNewChat();
                break;
            case 2:
                loadChats();
                break;
            case 3:
                sendFiles();
                break;
            case 4:
                signOut();
                break;
            default:
                signInMenu();
                break;
        }
    }

    /**
     * Loads a chat
     */
    public void loadChats() {
        int i=1;
        BigInteger[] tempChats;
        tempChats = new BigInteger[chats.size()];
        Console console = System.console();

        if (chats.size() == 0)
            System.out.println("You don't have any chat to show... Press enter to go back");
        else tempChats = printAndFillArrayChats();

        String option = console.readLine();
        if(!option.equals("")) {
            System.out.println(Integer.parseInt(option));
            BigInteger requiredChatId = tempChats[Integer.parseInt(option) - 1];
            if(chats.containsKey(requiredChatId))
                openChat(requiredChatId);
            else {
                Message message = new Message(GET_CHAT, getClientId(), RESPONSIBLE, requiredChatId.toString());
                actualState = Task.WAITING_FOR_CHAT;
                message.getBody();
                connection.sendMessage(message);
            }
        }
        else signInMenu();

    }

    public BigInteger[] printAndFillArrayChats(){

        int i=1;
        BigInteger[] tempChats;
        tempChats = new BigInteger[chats.size()];

        for (BigInteger chatId: chats.keySet()){
            tempChats[i-1] = chatId;
            System.out.println(i + ". " + chats.get(chatId).getChatName() + " Id: " + chatId);
            i++;
        }

        return tempChats;
    }

    public void sendFiles(){
        Console console = System.console();
        BigInteger[] tempChats;
        tempChats = new BigInteger[chats.size()];

        System.out.println("To send a file ... [ChatNumber]-[Path]  \n");
        tempChats = printAndFillArrayChats();

        String option = console.readLine();
        if(!option.equals("")) {

            String[] info = option.split("-");
            String path = info[1];

            System.out.println("path: " + path);

            String chatNumber = info[0];
            System.out.println("chatNumber " + chatNumber);

            BigInteger requiredChatId = tempChats[Integer.parseInt(chatNumber) - 1];
            System.out.println("chatId " + requiredChatId);

        }
        else signInMenu();
    }

    /**
     * Opens chat
     */
    public void openChat(BigInteger chatId) {

        Console console = System.console();
        actualState = Task.CHATTING;
        System.out.println("Opening chat ... ");

        if(chats.get(chatId)!=null){
            Chat chat = chats.get(chatId);
            String menu = "\n" + "\n" + "Chat:  " + chat.getChatName() + "\n";
            System.out.println(menu);
            printChatMessages(chatId);

            String alert = "\n Checking for new messages!!! \n";
            System.out.println(alert);
            printChatPendingMessages(chatId);

            String send = "\n \n Send a message: " + "\n" + "\n" + "\n" + "\n";
            System.out.println(send);
            currentChat = chatId.intValue();

            String messageToSend = console.readLine();
            while(!messageToSend.equals("")){
                Date date = new Date();
                ChatMessage chatMessage = new ChatMessage(chatId, date, getClientId(), messageToSend.getBytes(), TEXT_MESSAGE);
                chats.get(chatId).addChatMessage(chatMessage);
                Message message = new Message(NEW_MESSAGE, getClientId(), RESPONSIBLE, chatMessage, getClientId());
                connection.sendMessage(message);
                messageToSend = null;
                messageToSend = console.readLine();
            }

            signInMenu();
        }

    }

    public void printChatMessages(BigInteger chatId){
        for (ChatMessage message: chats.get(chatId).getChatMessages()) {
          System.out.println(new String(message.getContent()));
        }
    }

    public void printChatPendingMessages(BigInteger chatId){
        Iterator<ChatMessage> iter = chats.get(chatId).getChatPendingMessages().iterator();
        while(iter.hasNext()) {
            ChatMessage message = iter.next();
            if(message.getChatId().compareTo(chatId)==0) {
                getChat(chatId).addChatMessage(message);
                System.out.println(new String(message.getContent()));
                iter.remove();
            }
        }
    }

    /**
     * Creates a new chat
     */
    public void createNewChat() {
        Console console = System.console();
        actualState = WAITING_CREATE_CHAT;

        System.out.println("Name: ");
        String chatName = console.readLine();

        System.out.println("How many users do you want to invite?");
        int iterations = Integer.parseInt(console.readLine());
        int count;
        String[] names = new String[iterations];

        //TODO: Existe email??
        for(count = 0; count < iterations; count ++ ){
            System.out.println("Invite user to chat with you (email) : ");
            String participantEmail = console.readLine();
            names[count]=participantEmail;
            System.out.println("Invited participant " + count +" with email: "+ participantEmail);
        }

        Chat newChat = new Chat(email,chatName);
        newChat.addParticipant(email);


        for(int i=0;i<names.length;i++){
            newChat.addParticipant(names[i]);
        }

        addChat(newChat);
        Message message = new Message(CREATE_CHAT, getClientId(), RESPONSIBLE, newChat);
        connection.sendMessage(message);
    }

    /**
     * Sends a sign in message throw a ssl socket
     */
    public void signInUser() {
        actualState = WAITING_SIGNIN;
        String password = getCredentials();
        Message message = new Message(SIGNIN, getClientId(), NOT_RESPONSIBLE, email, createHash(password).toString());
        connection.sendMessage(message);
        //newConnectionAndSendMessage(message);
    }

    /**
     * Sends a sign up message throw a ssl socket
     */
    public void signUpUser() {
        actualState = WAITING_SIGNUP;
        String password = getCredentials();
        Message message = new Message(SIGNUP, getClientId(), NOT_RESPONSIBLE, email, createHash(password).toString());
        connection.sendMessage(message);
        //newConnectionAndSendMessage(message);
    }

    public void newConnectionAndSendMessage(Message message){
        connection = new ClientConnection(serverIp, serverPort, this);
        try {
            connection.connect();
        } catch (IOException e) {
            //Iniciar o protocolo
            System.out.println("\nError connecting");
        }

        //Listen
        threadPool.submit(connection);
        connection.sendMessage(message);
    }

    /**
     * Asks user for email and password
     *
     * @return String password
     */
    public String getCredentials() {

         /*The class Console has a method readPassword() that hides input.*/
        Console console = System.console();
        if (console == null) {
            System.err.println("No console.");
            System.exit(-1);
        }

        System.out.print("Email: ");
        email = console.readLine();
        console.printf(email + "\n");

        System.out.print("Password: ");

        char[] oldPassword = console.readPassword();

        return new String(oldPassword);
    }

    /**
     * Returns Client id
     *
     * @return client id
     */
    public BigInteger getClientId() {
        return createHash(email);
    }

    /**
     * Acts according off the actual state
     */
    public void verifyState(Message message) {

        if (message.getInitialServerPort() != serverPort || !message.getInitialServerAddress().equals(serverIp)) {
            if(message.getInitialServerPort() != -1){
                System.out.println("Meu server - porta: " + message.getInitialServerPort());
                System.out.println("Meu servidor - address: " + message.getInitialServerAddress());

                updateConnection(message.getInitialServerAddress(), message.getInitialServerPort());
            }
        }

        //TODO: How to do this???
        String body[] = new String[4];
        if(message.getBody()!=null)
             body = message.getBody().split(" ");

        switch (actualState) {
            case SIGNED_IN:
                signInMenu();
                break;
            case WAITING_SIGNUP:
            case WAITING_SIGNIN:
                if(message.getMessageType().equals(CLIENT_ERROR)){
                    printError(body[0]);
                    mainMenu();
                }
                else{
                    Message updateServerConnection = new Message(USER_UPDATED_CONNECTION, this.getClientId(), RESPONSIBLE);
                    connection.sendMessage(updateServerConnection);
                    actualState = SIGNED_IN;
                    signInMenu();
                }
                break;
            case WAITING_CREATE_CHAT:
                System.out.println("Creating chat " + body[0] + " ... Loading ...");
                openChat(new BigInteger(body[0]));
                break;
            case WAITING_FOR_CHAT:
                System.out.println("Received Chat");
                Chat chat = (Chat) message.getObject();
                chats.remove(chat.getIdChat());
                chats.put(chat.getIdChat(),chat);
                openChat(chat.getIdChat());
                break;
  /*          case RECEIVING_CHAT:
                System.out.println("Received Chat");
                Chat chatO = (Chat) message.getObject();
                chats.remove(chatO.getIdChat());
                chats.put(chatO.getIdChat(),chatO);
                break;*/
            case GET_CHATS:
                System.out.println("Get chats.....");
                Chat chatTemp = (Chat) message.getObject();
                if(chatTemp!=null)
                    chats.put(chatTemp.getIdChat(),chatTemp);
                break;
            case HOLDING:
                signInMenu();
                break;
            case WAITING_SIGNOUT:
                actualState = HOLDING;
                connection.stopTasks();
                connection.closeConnection();
                connection = null;
                System.out.println("\nSigned out!!");
                mainMenu();
                break;
            default:
                break;
        }
    }

    /**
     * Prints the error that comes from the server
     *
     * @param code error code
     */
    public void printError(String code) {
        switch (code) {
            case EMAIL_ALREADY_USED:
                System.out.println("\nEmail already exists. Try to sign in instead of sign up...");
                break;
            case EMAIL_NOT_FOUND:
                System.out.println("\nTry to create an account. Your email was not found on the database...");
                break;
            case WRONG_PASSWORD:
                System.out.println("\nImpossible to sign in, wrong email or password...");
                break;
            case ERROR_CREATING_CHAT:
                System.out.println("\nError creating chat...");
                break;
            case INVALID_USER_EMAIL:
                System.out.println("\nInvalid user email. Server couldn't find any user with that email ..");
                break;
            default:
                break;
        }
    }

    /**
     * Signs out the user
     */
    public void signOut() {
        actualState = WAITING_SIGNOUT;

        BigInteger clientId = getClientId();

        Message message = new Message(SIGNOUT, clientId, RESPONSIBLE, clientId.toString());
        connection.sendMessage(message);
        connection.closeConnection();
    }

    public enum Task {
        HOLDING, WAITING_SIGNIN, WAITING_SIGNUP, SIGNED_IN, CREATING_CHAT, WAITING_CREATE_CHAT,
        WAITING_SIGNOUT, WAITING_FOR_CHAT, RECEIVING_CHAT, CHATTING, GET_CHATS
    }

    public void addChat(Chat chat){
        System.out.println("Added new Chat with chat name: " + chat.getChatName());
        chats.put(chat.getIdChat(),chat);
    }

    public Chat getChat(BigInteger chatId){
       return chats.get(chatId);
    }

    public void printClientChats(){
        chats.forEach((k, v) -> System.out.println("Chat : " + k));
    }

    public int getCurrentChat() {
        return currentChat;
    }

    public void askForChat(BigInteger chatId){
        Message message = new Message(GET_CHAT, getClientId(), RESPONSIBLE, chatId.toString());
        actualState = Task.RECEIVING_CHAT;
        connection.sendMessage(message);
    }

    public void askForClientChats(){

        System.out.println("Loading your chats ... ");
        Message message = new Message(GET_ALL_CHATS, getClientId(), RESPONSIBLE);
        actualState = Task.GET_CHATS;
        connection.sendMessage(message);
        //Message temp = connection.receiveMessage();
        //System.out.println(temp.getMessageType());
    }

    public void askForPendingChats(){
        System.out.println("Checking for new chats ... ");
        Message message = new Message(GET_ALL_PENDING_CHATS, getClientId(), RESPONSIBLE);
        connection.sendMessage(message);
       // Message temp = connection.receiveMessage();
        //System.out.println(temp.getMessageType());
    }

    public void updateConnection(String newServerIp, int newServerPort) {

        serverPort = newServerPort;
        serverIp = newServerIp;

        connection.stopTasks();
        connection.closeConnection();

        connection = new ClientConnection(serverIp, serverPort, this);
        try {
            connection.connect();
        } catch (IOException e) {
            //Iniciar o protocolo
            System.out.println("\nError connecting");
        }
        threadPool.submit(connection);
        Message connectToServer = new Message(USER_UPDATED_CONNECTION, this.getClientId(), RESPONSIBLE);
        connection.sendMessage(connectToServer);

    }
}
