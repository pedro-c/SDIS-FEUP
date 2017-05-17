package Client;

import Chat.Chat;
import Messages.Message;
import Messages.MessageHandler;
import Server.Server;
import Server.ServerChat;

import java.io.Console;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Client.Client.Task.*;
import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;
import static Utilities.Utilities.getTimestamp;

public class Client {

    private Scanner scannerIn;
    private String email;
    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private int serverPort;
    private String serverIp;
    private Hashtable<BigInteger, Chat> userChats;
    private MessageHandler messageHandler;
    private Task atualState;

    /**
     * Client
     */
    public Client(String serverIp, int serverPort) {
        this.serverPort = serverPort;
        this.serverIp = serverIp;
        this.userChats = new Hashtable<BigInteger, Chat>();
        this.atualState = Task.HOLDING;

        scannerIn = new Scanner(System.in);
    }

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
                atualState = CREATING_CHAT;
                createNewChat();
                break;
            case 2:
                break;
            case 3:
                signOut();
                break;
            default:
                System.exit(0);
        }
    }

    /**
     * Opens chat
     */
    public void openChat(Chat chat) {
        String menu = "\n" + "\n" + "Chat:  " + chat.getChatName() + "\n" + "\n" + "Send a message to " + chat.getParticipant_email() + "\n" + "\n" + "\n" + "\n";
        System.out.println(menu);
    }

    /**
     * Creates a new chat
     */
    public void createNewChat() {
        Console console = System.console();
        atualState = Task.WAITING_CREATE_CHAT;

        System.out.println("Name: ");
        String chatName = console.readLine();
        System.out.println("Invite user to chat with you (email) : ");
        String participantEmail = console.readLine();

        while (participantEmail == null || participantEmail.equals(email)) {
            System.out.println("You must invite one user to chat with you (email). ");
            participantEmail = console.readLine();
        }

        Chat newChat = new Chat(generateChatId(), email);
        if (chatName != null)
            newChat.setChatName(chatName);
        newChat.setParticipant_email(participantEmail);

        userChats.put(newChat.getIdChat(),newChat);

        System.out.println(newChat.getIdChat());

        Message message = new Message(CREATE_CHAT, getClientId(), newChat);
        messageHandler.setMessage(message);
        threadPool.submit(messageHandler);

    }

    /**
     * Generates Chat id with creation date and user_id : CREATION_DATE + USER_CREATOR_ID
     *
     * @return BigInteger chat Id
     */
    public BigInteger generateChatId() {
        return createHash(String.valueOf(getTimestamp()) + email);
    }

    /**
     * Sends a sign in message throw a ssl socket
     */
    public void signInUser() {
        atualState = Task.WAITING_SIGNIN;
        String password = getCredentials();
        Message message = new Message(SIGNIN, getClientId(), email, createHash(password).toString());
        messageHandler = new MessageHandler(message, serverIp, serverPort, this);
        threadPool.submit(messageHandler);

    }

    /**
     * Sends a sign up message throw a ssl socket
     */
    public void signUpUser() {
        atualState = Task.WAITING_SIGNUP;
        String password = getCredentials();
        Message message = new Message(SIGNUP, getClientId(), email, createHash(password).toString());
        messageHandler = new MessageHandler(message, serverIp, serverPort, this);
        threadPool.submit(messageHandler);
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
     *
     * @param response response message
     */
    public void verifyState(Message response) {
        switch (atualState) {
            case WAITING_SIGNIN:
            case WAITING_SIGNUP:
                if (response.getMessageType().equals(CLIENT_SUCCESS)) {
                    atualState = SIGNED_IN;
                    signInMenu();
                } else {
                    atualState = HOLDING;
                    printError(response.getBody());
                    mainMenu();
                }
                break;
            case WAITING_CREATE_CHAT:
                if (response.getMessageType().equals(CLIENT_SUCCESS)) {
                    System.out.println(response.getBody());
                    Chat chat = userChats.get(new BigInteger(response.getBody()));
                    openChat(chat);
                } else {
                    atualState = HOLDING;
                    printError(response.getBody());
                    signInMenu();
                }
                break;
            case WAITING_SIGNOUT:
                atualState = HOLDING;
                System.out.println("\nSigned out!!");
                mainMenu();
                break;
            case SIGNED_IN:
                if (response.getMessageType().equals(NEW_CHAT_INVITATION)) {
                    System.out.println("Received new chat invitation!!!");
                    ServerChat sv = (ServerChat) response.getObject();
                    Chat chat = new Chat(sv.getIdChat(),sv.getCreatorEmail());
                    userChats.put(chat.getIdChat(),chat);
                    openChat(chat);
                }
                else {
                    System.out.println("Error");
                }
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
        atualState = WAITING_SIGNOUT;

        BigInteger clientId = getClientId();

        Message message = new Message(SIGNOUT, clientId, clientId.toString());
        messageHandler = new MessageHandler(message, serverIp, serverPort, this);
        threadPool.submit(messageHandler);
    }

    /**
     * Sets server port
     *
     * @param serverPort
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * Sets server ip
     *
     * @param serverIp
     */
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }


    /**
     * Gets client current task
     * @return
     */
    public Client.Task getAtualState() {
        return atualState;
    }

    public enum Task {
        HOLDING, WAITING_SIGNIN, WAITING_SIGNUP, SIGNED_IN, CREATING_CHAT, WAITING_CREATE_CHAT,
        WAITING_SIGNOUT
    }
}
