package Messages;

import Client.Client;
import Server.Server;
import Server.Node;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static Utilities.Constants.PREDECESSOR;

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
        sendMessage(message);
        receiveResponse();
        closeSocket();
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
     * Reads a message response from the socket and calls the handler function
     */
    public void receiveResponse(){
        Message response = null;
        try {
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


    public void handleResponse(Message response){

        if(response.getMessageType().equals(PREDECESSOR)){
            String[] nodeInfo = response.getBody().split(" ");
            Node node = new Node(nodeInfo[0],nodeInfo[1]);
            this.server.setPredecessor(node);
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

}
