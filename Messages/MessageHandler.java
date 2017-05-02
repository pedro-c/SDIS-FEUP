package Messages;

import Client.Client;
import Server.Server;
import Server.Node;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

import static Utilities.Constants.PREDECESSOR;

public class MessageHandler implements Runnable {

    Message message;
    String ip;
    int port;
    Server server = null;
    Client client = null;
    private SSLSocket sslSocket;
    private SSLSocketFactory sslSocketFactory;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;


    public MessageHandler(Message message, String ip, String port, Server server) {

        this.message = message;
        this.ip = ip;
        this.port = Integer.parseInt(port);
        this.server = server;
        run();
    }

    public MessageHandler(Message message, String ip, String port, Client client) {

        this.message = message;
        this.ip = ip;
        this.port =  Integer.parseInt(port);
        this.client = client;
        run();
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
     *
     */
    public void sendMessage(Message message) {
        try {
            outputStream.writeObject(this.message);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
