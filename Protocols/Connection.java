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
    public void connect() throws IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);
        sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
        outputStream = new ObjectOutputStream(sslSocket.getOutputStream());
        inputStream = new ObjectInputStream(sslSocket.getInputStream());
    }

    /**
     * Sends a message
     * @param message message to be sent
     */
    public void sendMessage(Message message) throws IOException {

        if (message == null)
            throw new IOException();
        outputStream.writeObject(message);

    }

    /**
     * Receives a message
     * @return message received
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {

        return (Message) inputStream.readObject();

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

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}