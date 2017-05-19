package Protocols;

import Messages.Message;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;

/**
 * Handles connections
 */
public class Connection {

    protected SSLSocket sslSocket;
    protected BufferedReader in;
    protected ObjectInputStream inputStream;
    protected ObjectOutputStream outputStream;

    protected String ip;
    protected int port;

    protected ExecutorService service = Executors.newFixedThreadPool(MAX_NUMBER_OF_THREADS);

    public Connection(String ip, int port) {

        this.ip = ip;
        this.port = port;
    }

    /**
     * Handles new SSL Connections to the server
     *
     * @param socket
     */
    public Connection(SSLSocket socket) {
        this.sslSocket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            outputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            inputStream = new ObjectInputStream(sslSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error creating buffered reader...");
        }
    }

    /**
     * Connects to a certain ip and port
     */
    public void connect() {

        try {
            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);
            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            outputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            inputStream = new ObjectInputStream(sslSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error connecting to the server ssl socket...");
        }

    }

    /**
     * Sends a message
     * @param message message to be sent
     */
    public void sendMessage(Message message) {

        try {
            if (message == null)
                throw new IOException();
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("\nError sending message...");
        }
    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage(){

        try {
            return (Message) inputStream.readObject();
        } catch (IOException e) {
            System.out.println("\nError receiving message or connection closed");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("\nError receiving message...");
        }

        return null;
    }

    /**
     * Close the connection
     */
    public void closeConnection() {
        try {
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("\nFailed to close ssl connection");
        }
    }

    public void stopTasks(){
        service.shutdownNow();
    }
}