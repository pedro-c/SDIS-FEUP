package Server;

import Messages.Message;
import Chat.Chat;
import Messages.MessageHandler;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;

import static Utilities.Constants.*;

/**
 * Handles new SSL Connections to the server
 */
public class ConnectionHandler implements Runnable {

    private SSLSocket sslSocket;
    private BufferedReader in;
    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Server server;

    /**
     * Handles new SSL Connections to the server
     * @param socket
     * @param server
     */
    public ConnectionHandler(SSLSocket socket, Server server) {
        this.sslSocket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            serverOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            serverInputStream = new ObjectInputStream(sslSocket.getInputStream());
        } catch (IOException e) {
            System.out.println("Error creating buffered reader...");
            e.printStackTrace();
        }
    }

    /**
     * Sends a message through a ssl socket
     */
    public void sendMessage(Message message) {
        try {
            serverOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Analyses Responses
     * @param response
     */
    public Message analyseResponse(Message response) {
        String[] body;
        System.out.println(response.getMessageType());

        switch (response.getMessageType()) {
            case SIGNIN:
                 body = response.getBody().split(" ");
                return server.loginUser(body[0], body[1]);
            case SIGNUP:
                 body = response.getBody().split(" ");
                return server.addUser(body[0],body[1]);
            case CREATE_CHAT:
                return server.createChat((Chat) response.getObject());
            case NEWNODE:
                body = response.getBody().split(" ");
                server.newNode(body);
                break;
            case PREDECESSOR:
                Node temp = (Node) response.getObject();
                server.setPredecessor(temp);
                server.sendFingerTableToSuccessor();
                break;
            case SUCCESSOR_FT:
                server.updateFingerTableFromSuccessor((ArrayList<Node>) response.getObject());
                break;
            default:
                break;
        }
        return null;
    }

    public void closeConnection(){
        try {
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to close ssl connection");
        }
    }


    /**
     * Reads Messages
     */
    public void run() {
        Message message = null;
        try {
            message = (Message) serverInputStream.readObject();
            Message responseMessage = analyseResponse(message);
            sendMessage(responseMessage);
        } catch (IOException e) {
            System.out.println("Error reading message...");
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading message...");
        }

    }
}