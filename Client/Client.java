package Client;

import Chat.Chat;
import Chat.ChatMessage;
import Messages.Message;
import Protocols.ClientConnection;
import Server.User;

import java.io.Console;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
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
    private int serverPort;
    private String serverIp;
    private Task actualState;
    private ConcurrentHashMap<BigInteger, Chat> chats;

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

    /**
     * Main
     * @param args initial arguments
     */
    public static void main(String[] args) {

        if (args.length != 2) {
            throw new IllegalArgumentException("\nUsage : java Client.Client <serverIp> <serverPort>");
        }

        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);

        if (serverPort < 0 && serverPort > 65535) {
            throw new IllegalArgumentException("\nThe port needs to be between 0 and 65535");
        }

        Client client = new Client(serverIp, serverPort);
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
        String menu = "\n Menu " + "\n 1. Create a new Chat" + "\n 2. Open Chat" + "\n 3. Sign Out" + "\n";
        System.out.println(menu);
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
                signOut();
                break;
            default:
                signInMenu();

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
        else {
            for (BigInteger chatId: chats.keySet()){
                tempChats[i-1] = chatId;
                System.out.println(i + ". " + chats.get(chatId).getChatName() + " Id: " + chatId);
                i++;
            }
        }

        String option = console.readLine();
        if(!option.equals("")) {
            System.out.println(Integer.parseInt(option));
            BigInteger requiredChatId = tempChats[Integer.parseInt(option) - 1];
            Message message = new Message(GET_CHAT, getClientId(), requiredChatId.toString());
            actualState = Task.WAITING_FOR_CHAT;
            message.getBody();
            connection.sendMessage(message);
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
            String menu = "\n" + "\n" + "Chat:  " + chat.getChatName() + "\n" + "\n" + getLastMessages(chatId) + "\n" + "Send a message: " + "\n" + "\n" + "\n" + "\n";
            System.out.println(menu);

            System.out.println(getLastMessages(chatId));


            String messageToSend = console.readLine();
            while(!messageToSend.equals("")){
                System.out.println(1);
                Date date = new Date();
                ChatMessage chatMessage = new ChatMessage(chatId, date, getClientId(), messageToSend.getBytes(), TEXT_MESSAGE);
                Message message = new Message(NEW_MESSAGE, getClientId(), chatMessage, getClientId());
                connection.sendMessage(message);
                messageToSend = null;
                messageToSend = console.readLine();
            }

            signInMenu();

        }

    }

    public String getLastMessages(BigInteger chatId){

        String messagesToprint = null;
        if(chats.get(chatId)!=null) {

            Chat chat = chats.get(chatId);
            if(chat.getChatMessages().size()==0){
                messagesToprint = "No messages to see ... ";
                return messagesToprint;
            }

            for(ChatMessage message:  chat.getChatMessages()){
                 System.out.println("Loading messages...");
                 messagesToprint.join(new String(message.getContent()), "\n \n");
            }
        }

        return messagesToprint;
    }

    /**
     * Creates a new chat
     */
    public void createNewChat() {
        Console console = System.console();
        actualState = WAITING_CREATE_CHAT;

        System.out.println("Name: ");
        String chatName = console.readLine();
        System.out.println("Invite user to chat with you (email) : ");
        String participantEmail = console.readLine();

        while (participantEmail == null || participantEmail.equals(email)) {
            System.out.println("You must invite one user to chat with you (email). ");
            participantEmail = console.readLine();
        }

        System.out.println("1: " + chatName);
        Chat newChat = new Chat(email,chatName);

        newChat.addParticipant(participantEmail);
        newChat.addParticipant(email);
        addChat(newChat);
        Message message = new Message(CREATE_CHAT, getClientId(), newChat);
        connection.sendMessage(message);
    }

    /**
     * Sends a sign in message throw a ssl socket
     */
    public void signInUser() {
        actualState = WAITING_SIGNIN;
        String password = getCredentials();
        Message message = new Message(SIGNIN, getClientId(), email, createHash(password).toString());
        newConnectionAndSendMessage(message);
    }

    /**
     * Sends a sign up message throw a ssl socket
     */
    public void signUpUser() {
        actualState = WAITING_SIGNUP;
        String password = getCredentials();
        Message message = new Message(SIGNUP, getClientId(), email, createHash(password).toString());
        newConnectionAndSendMessage(message);
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

                serverPort = message.getInitialServerPort();
                serverIp = message.getInitialServerAddress();

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
                Message connectToServer = new Message(USER_UPDATED_CONNECTION, this.getClientId());
                connection.sendMessage(connectToServer);
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
                System.out.println("Chat " + chat.getIdChat());
                chats.remove(chat);
                chats.put(chat.getIdChat(),chat);
                openChat(chat.getIdChat());
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

        Message message = new Message(SIGNOUT, clientId, clientId.toString());

        connection.sendMessage(message);
    }

    public enum Task {
        HOLDING, WAITING_SIGNIN, WAITING_SIGNUP, SIGNED_IN, CREATING_CHAT, WAITING_CREATE_CHAT,
        WAITING_SIGNOUT, WAITING_FOR_CHAT, CHATTING
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

}
