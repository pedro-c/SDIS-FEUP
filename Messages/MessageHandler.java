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

    }

    public MessageHandler(Message message, String ip, String port, Client client) {

        this.message = message;
        this.ip = ip;
        this.port = port;
        this.client = client;

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
     * @param message message to send
     */
    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        closeSocket();
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
