package Client;

import Chat.Chat;
import Messages.Message;
import Protocols.ClientConnection;
import Server.User;
import java.io.Console;
import java.math.BigInteger;
import java.util.Scanner;
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
        connection.connect();

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
                System.exit(0);
        }
    }

    /**
     * Logged in user menu
     */
    public void signInMenu() {
        String menu = "\n Menu " + "\n 1. Create a new Chat" + "\n 2. Open Chat" + "\n 3. Sign Out" + "\n";
        System.out.println(menu);
        int option = scannerIn.nextInt();
        switch (option) {
            case 1:
                actualState = CREATING_CHAT;
                createNewChat();
                break;
            case 2:
                loadChat();
                break;
            case 3:
                signOut();
                break;
            default:
                System.exit(0);
        }
    }

    /**
     * Loads a chat
     */
    public void loadChat() {
        if (chats.size() == 0)
            System.out.println("You don't have any chat to show...");
        else chats.forEach((k, v) -> System.out.println(v.getChatName() + "\n"));
    }

    /**
     * Opens chat
     */
    public void openChat(Chat chat) {
        String menu = "\n" + "\n" + "Chat:  " + chat.getChatName() + "\n" + "\n" + "Send a message: " + "\n" + "\n" + "\n" + "\n";
        System.out.println(menu);
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

        Chat newChat = new Chat(email);
        if (chatName != null)
            newChat.setChatName(chatName);

        newChat.addParticipant(participantEmail);
        newChat.addParticipant(email);

        chats.put(newChat.getIdChat(), newChat);

        System.out.println(newChat.getIdChat());
        actualState = WAITING_CREATE_CHAT;
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
        connection.connect();

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

            serverPort = message.getInitialServerPort();
            serverIp = message.getInitialServerAddress();

            connection.stopTasks();
            connection.closeConnection();

            connection = new ClientConnection(serverIp, serverPort, this);
            connection.connect();
            threadPool.submit(connection);
        }

        String body[] = message.getBody().split(" ");

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
                else
                    signInMenu();
                break;
            case CREATING_CHAT:
                //openChat(pendingChat);
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
        WAITING_SIGNOUT
    }
}
