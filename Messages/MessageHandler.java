package Messages;

import Client.Client;
import Server.Server;
import Server.Node;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;

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
    private Message message;

    public MessageHandler(Message message, String ip, String port, Server server) {

        this.ip = ip;
        this.port = Integer.parseInt(port);
        this.server = server;
        this.message = message;
    }

    public MessageHandler(Message message, String ip, String port, Client client) {

        this.ip = ip;
        this.port =  Integer.parseInt(port);
        this.client = client;
        this.message = message;
    }

    public void run() {
        connectToServer();
        sendMessage(message);
        while(true){
            receiveResponse();
        }
    }

    /**
     * Connects to server
     */
    public void connectToServer() {

        try {
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(ip), port);
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

    /**
     * Handles with the responses
     * @param response
     */
    public void handleResponse(Message response){

        String[] nodeInfo;

        System.out.println("Message received: " + response.getMessageType());

        switch (response.getMessageType()){
            //PREDECESSOR NodeId NodeIp NodePort
            case PREDECESSOR:
                nodeInfo = response.getBody().split(" ");
                this.server.setPredecessor(new Node(nodeInfo[1],nodeInfo[2],Integer.parseInt(nodeInfo[0])));
                break;
            case CLIENT_SUCCESS:
            case CLIENT_ERROR:
                client.verifyState(response);
                break;
            //SUCCESSOR NodeId NodeIp NodePort
            case SUCCESSOR:
                nodeInfo = response.getBody().split(" ");
                Node successor = new Node(nodeInfo[1],nodeInfo[2],Integer.parseInt(nodeInfo[0]));
                this.server.updateFingerTable(successor);
                break;
            //NEWNODE_ANSWER NodeId NodeIp NodePort
            //Talk to the node with that ip an port
            case NEWNODE_ANSWER:
                nodeInfo = response.getBody().split(" ");
                Node nextNode = new Node(nodeInfo[1],nodeInfo[2],Integer.parseInt(nodeInfo[0]));
                this.server.joinNetwork(nextNode);
                break;
            default:
                break;
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
