package Client;

import Messages.Message;
import Messages.MessageHandler;

import java.io.*;
import java.math.BigInteger;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

public class Client {

    private Scanner scannerIn;
    private String email;
    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private int serverPort;
    private String serverIp;

    public enum Task {
        HOLDING, WAITING_SIGNIN, WAITING_SIGNUP, SIGNED_IN
    }

    private Task atualState;

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
     * Client
     */
    public Client(String serverIp, int serverPort) {

        this.serverPort = serverPort;
        this.serverIp = serverIp;

        this.atualState = Task.HOLDING;

        scannerIn = new Scanner(System.in);
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

    public void signInMenu() {
        String menu = "\n Menu " + "\n 1. Create a new Chat" + "\n 2. Open Chat" + "\n 3. Sign Out";
        System.out.println(menu);
        int option = scannerIn.nextInt();
        switch (option) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                atualState = Task.HOLDING;
                mainMenu();
                break;
            default:
                System.exit(0);
        }
    }

    /**
     * Sends a sign in message throw a ssl socket
     */
    public void signInUser() {
        atualState = Task.WAITING_SIGNIN;
        String password = getCredentials();
        Message message = new Message(SIGNIN, getClientId(), email, createHash(password).toString());
        MessageHandler handler = null;
        handler = new MessageHandler(message, serverIp, Integer.toString(serverPort), this);
        threadPool.submit(handler);

    }

    /**
     * Sends a sign up message throw a ssl socket
     */
    public void signUpUser() {
        atualState = Task.WAITING_SIGNUP;
        String password = getCredentials();
        Message message = new Message(SIGNUP, getClientId(), email, createHash(password).toString());
        MessageHandler handler = null;
        handler = new MessageHandler(message, serverIp, Integer.toString(serverPort), this);
        threadPool.submit(handler);
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
        String password = new String(oldPassword);

        return password;
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
     * @param response response message
     */
    public void verifyState(Message response){
        switch (atualState){
            case WAITING_SIGNIN:
            case WAITING_SIGNUP:
                if(response.getMessageType().equals(CLIENT_SUCCESS)){
                    atualState = Task.SIGNED_IN;
                    signInMenu();
                }
                else {
                    atualState = Task.HOLDING;
                    printError(response.getBody());
                    mainMenu();
                }
                break;
            default:
                break;
        }
    }

    /**
     * Prints the error that comes from the server
     * @param code error code
     */
    public void printError(String code){
        switch (code){
            case EMAIL_ALREADY_USED:
                System.out.println("\nEmail already exists. Try to sign in instead of sign up...");
                break;
            case EMAIL_NOT_FOUND:
                System.out.println("\nTry to create an account. Your email was not found on the database...");
                break;
            case WRONG_PASSWORD:
                System.out.println("\nImpossible to sign in, wrong email or password...");
                break;
            default:
                break;
        }
    }
}
