package Messages;

import Client.Client;
import Server.Server;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class MessageHandler implements Runnable {

    Message message;
    String ip;
    String port;
    Server server = null;
    Client client = null;
    private SSLSocket sslSocket;
    private SSLSocketFactory sslSocketFactory;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;


    public MessageHandler(Message message, String ip, String port, Server server) {

        this.message = message;
        this.ip = ip;
        this.port = port;
        this.server = server;

        run();

    }

    public MessageHandler(Message message, String ip, String port, Client client) {

        this.message = message;
        this.ip = ip;
        this.port = port;
        this.client = client;

        run();

    }

    public void run() {
        connectToServer();
    }

    /**
     * Connects to server
     */
    public void connectToServer() {

        try {
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost", 4445);
            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            outputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            inputStream = new ObjectInputStream(sslSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("Error creating ssl socket...");
            e.printStackTrace();
        }

    }

    /**
     * Sends a message through a ssl socket
     *
     */
    public void sendMessage() {
        try {
            outputStream.writeObject(this.message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receives a message through a ssl socket
     *
     */
    public Message receiveMessage(){
        Message message = null;
        try {
            message = (Message) inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return message;
    }

    /**
     * Closes socket
     */
    public void closeSocket() {
        try {
            sslSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing ssl socket...");
            e.printStackTrace();
        }
    }

}
