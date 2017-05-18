package Messages;

import Chat.Chat;
import Client.Client;
import Server.ConnectionHandler;
import Server.Node;
import Server.Server;
import Server.ServerChat;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
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
        receiveResponse();
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
                if (client.getAtualState() == Client.Task.WAITING_SIGNUP || client.getAtualState() == Client.Task.WAITING_SIGNIN) {
                    System.out.println(1);
                    client.setServerIp(response.getInitialServerAddress());
                    client.setServerPort(response.getInitialServerPort());
                    client.setAtualState(Client.Task.SIGNED_IN);
                    System.out.println("Logged in with success..");
                }
                else if (client.getAtualState() == Client.Task.WAITING_CREATE_CHAT){
                    System.out.println("Chat criado");
                    client.setAtualState(Client.Task.CREATING_CHAT);
                    System.out.println("Chat criado");
                    client.setPendingChat(new BigInteger(response.getBody()));
                    System.out.println("Chat criado");
                }
                else {
                    System.out.println("Sending message back to initiator server");
                    connectionHandler.sendMessage(response);
                }
                break;
            case CLIENT_ERROR:
                if (client.getAtualState() == Client.Task.WAITING_SIGNUP || client.getAtualState() == Client.Task.WAITING_SIGNIN) {
                    client.setAtualState(Client.Task.HOLDING);
                }
                else if (client.getAtualState() == Client.Task.WAITING_CREATE_CHAT){
                    client.setAtualState(Client.Task.HOLDING);
                }
                break;
            case NEW_CHAT_INVITATION:
                if (response.getMessageType().equals(NEW_CHAT_INVITATION)) {
                    System.out.println("Received new chat invitation..");
                    ServerChat sv = (ServerChat) response.getObject();
                    Chat chat = new Chat(sv.getIdChat(), sv.getCreatorEmail());
                    client.addPendingChat(chat);
                } else {
                    System.out.println("Error");
                }
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

    public void setMessage(Message message) {
        this.message = message;
    }

}
