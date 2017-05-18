package Messages;

import Client.Client;
import Server.ConnectionHandler;
import Server.Node;
import Server.Server;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static Utilities.Constants.*;

public class MessageHandler implements Runnable {

    String ip;
    int port;
    Server server = null;
    Client client = null;
    private SSLSocket sslSocket;
    private SSLSocketFactory sslSocketFactory;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private ConnectionHandler connectionHandler;

    private Message message;

    public MessageHandler(Message message, String ip, int port, Server server) {

        this.ip = ip;
        this.port = port;
        this.server = server;
        this.message = message;
    }

    public MessageHandler(Message message, String ip, int port, Client client) {

        this.ip = ip;
        this.port = port;
        this.client = client;
        this.message = message;
    }

    public MessageHandler(Message message, String ip, int port, ConnectionHandler connectionHandler) {

        this.ip = ip;
        this.port = port;
        this.client = client;
        this.message = message;
        this.connectionHandler = connectionHandler;
    }

    public void run() {
        connectToServer();
        sendMessage(message);
        while (true) {
            System.out.println("Reading response...");
            receiveResponse();
        }
    }

    public void listen() {
        while (true) {
            System.out.println("Reading response...");
            receiveResponse();
        }
    }

    /**
     * Connects to server
     */
    public void connectToServer() {

        try {
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(ip, port);
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
     */
    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message through a ssl socket
     */
    public void sendMessage() {
        try {
            outputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a message response from the socket and calls the handler function
     */
    public void receiveResponse() {
        Message response = null;
        try {
            System.out.println("Trying to receive message...");
            response = (Message) inputStream.readObject();
            handleResponse(response);
        } catch (IOException e) {
            System.out.println("Error reading message...");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading message...");
            e.printStackTrace();
        }
    }

    /**
     * Handles with the responses
     *
     * @param response
     */
    public void handleResponse(Message response) {

        String[] nodeInfo;

        System.out.println("MH Message received: " + response.getMessageType());

        switch (response.getMessageType()) {
            case PREDECESSOR:
                nodeInfo = response.getBody().split(" ");
                this.server.getDht().setPredecessor(new Node(nodeInfo[1], Integer.parseInt(nodeInfo[2]), Integer.parseInt(nodeInfo[0])));
                break;
            case CLIENT_SUCCESS:
                if (client != null) {
                    client.setServerIp(response.getInitialServerAddress());
                    client.setServerPort(response.getInitialServerPort());
                    client.verifyState(response);
                } else {
                    System.out.println("Sending message back to initiator server");
                    connectionHandler.sendMessage(response);
                }
                break;
            case CLIENT_ERROR:
                client.verifyState(response);
                break;
            default:
                break;
        }

        System.out.println("I'm blocked on signinMenu, waiting for an user input");
        if (client.getAtualState() == Client.Task.SIGNED_IN || client.getAtualState() == Client.Task.SIGNED_IN) {
            System.out.println("VOU OUVIR");
            listen();
        }

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

    public void setMessage(Message message) {
        this.message = message;
    }

}
