package Server;

import Chat.Chat;
import Messages.Message;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.ArrayList;

import static Utilities.Constants.*;

/**
 * Handles new SSL Connections to the server
 */
public class ConnectionHandler implements Runnable {

    public SSLSocket sslSocket;
    private BufferedReader in;
    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Server server;

    /**
     * Handles new SSL Connections to the server
     *
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
        System.out.println("Sending message with type: " + message.getMessageType() + " and body " + message.getBody());
        try {
            serverOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Analyses Responses
     *
     * @param response
     */
    public Message analyseResponse(Message response) {
        String[] body;
        System.out.println(response.getMessageType());

        switch (response.getMessageType()) {
            case SIGNIN:
                body = response.getBody().split(" ");
                System.out.println("REQUEST ID: " + response.getSenderId().intValue());
                if (server.isResponsibleFor(response.getSenderId())) {
                    Message message = server.loginUser(body[0], body[1]);
                    message.setInitialServerAddress(server.getNodeIp());
                    message.setInitialServerPort(server.getNodePort());
                    return message;
                }
                else {
                    System.out.println("REDIRECTING ID: " + response.getSenderId().intValue());
                    server.redirect(response);
                }
                break;
            case SIGNUP:
                body = response.getBody().split(" ");
                System.out.println("REQUEST ID: " + response.getSenderId().intValue());
                if (server.isResponsibleFor(response.getSenderId())) {
                    server.saveConnection(this.sslSocket, response.getSenderId());
                    server.addUser(body[0], body[1]);
                    closeConnection();
                } else {
                    System.out.println("REDIRECTING ID: " + response.getSenderId().intValue());
                    server.redirect(response);
                    closeConnection();
                }
                break;
            case CREATE_CHAT:
                if(server.isResponsibleFor(response.getSenderId()))
                    server.createChat((Chat) response.getObject());
                else {
                    System.out.println("REDIRECTING ID: " + response.getSenderId().intValue());
                    server.redirect(response);
                }
                break;
            case NEWNODE:
                body = response.getBody().split(" ");
                server.newNode(body);
                server.printFingerTable();
                break;
            case PREDECESSOR:
                Node temp = (Node) response.getObject();
                server.setPredecessor(temp);
                server.sendFingerTableToSuccessor();
                server.printFingerTable();
                break;
            case SUCCESSOR_FT:
                ArrayList<Node> ft = (ArrayList<Node>) response.getObject();
                server.updateFingerTableFromSuccessor(ft);
                server.setPredecessor(ft.get(0));
                server.printFingerTable();
                break;
            default:
                break;
        }
        return null;
    }

    public void closeConnection() {
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