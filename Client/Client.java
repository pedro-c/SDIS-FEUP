package Client;

import Messages.Message;
import Utilities.Constants;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.Scanner;
import static Utilities.Utilities.createHash;

public class Client {

    private Scanner scannerIn;
    private SSLSocket sslSocket;
    private SSLSocketFactory sslSocketFactory;
    private BufferedReader in;
    private PrintWriter out;
    private ObjectInputStream clientInputStream;
    private ObjectOutputStream clientOutputStream;
    private String email;

    public static void main(String[] args){
        Client client = new Client();
        client.mainMenu();
    }

    /**
     * Client
     */
    public Client(){
        scannerIn = new Scanner(System.in);
    }

    /**
     * Prints main menu
     */
    public void mainMenu() {
        String menu = "\n Menu " + "\n 1. Sign in" + "\n 2. Sign up" + "\n 3. Exit" + "\n";
        System.out.println(menu);
        int option = scannerIn.nextInt();
        switch (option){
            case 1:
                signInUser();
                break;
            case 2:
                signUpUser();
                break;
            case 3:
                System.exit(0);
        }
    }

    /**
     * Sends a sign in message throw a ssl socket
     */
    public void signInUser(){
        String password = getCredentials();
         sendMessage(new Message(Constants.SIGNIN.getBytes(), getClientId(), email, password));
    }

    /**
     * Sends a sign up message throw a ssl socket
     */
    public void signUpUser(){
        String password = getCredentials();
        sendMessage(new Message(Constants.SIGNUP.getBytes(), getClientId(), email, password));
    }

    /**
     * Asks user for email and password
     * @return String password
     */
    public String getCredentials(){

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
        char [] oldPassword = console.readPassword();
        String password = new String(oldPassword);

        return password;
    }

    /**
     * Sends a message throw a ssl socket
     * @param message message to send
     */
    public void sendMessage(Message message){
        connectToServer();
        try {
            clientOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        receiveMessage();
    }

    public void receiveMessage(){

        Message message = null;

        try {
            message = (Message) clientInputStream.readObject();
            analyseResponse(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        closeSocket();
    }


    public void analyseResponse(Message message){

    }

    /**
     * Connects to server
     */
    public void connectToServer(){

        try {
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost",4445);
            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            clientOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            clientInputStream =  new ObjectInputStream(sslSocket.getInputStream());
           in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
           out = new PrintWriter(sslSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error creating ssl socket...");
            e.printStackTrace();
        }

    }

    /**
     * Closes socket
     */
    public void closeSocket(){
        try {
            sslSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing ssl socket...");
            e.printStackTrace();
        }
    }

    /**
     * Returns Client id
     * @return client id
     */
    public byte[] getClientId(){
        return createHash(email);
    }
}
