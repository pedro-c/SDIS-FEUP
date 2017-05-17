package Server;

import Chat.Chat;
import Messages.Message;
import Messages.MessageHandler;

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

        System.out.println("CH sending message...");
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

        System.out.println("CH receiving message...");

        switch (response.getMessageType()) {
            case SIGNIN:
                body = response.getBody().split(" ");
                System.out.println("REQUEST ID: " + Integer.remainderUnsigned(response.getSenderId().intValue(), 128));
                if (server.isResponsibleFor(response.getSenderId())) {
                    System.out.println("I'm the RESPONSIBLE server");
                    Message message = server.loginUser(body[0], body[1]);
                    message.setInitialServerAddress(server.getNodeIp());
                    message.setInitialServerPort(server.getNodePort());
                    return message;
                } else {
                    System.out.println("REDIRECTING ID: " + Integer.remainderUnsigned(response.getSenderId().intValue(), 128));
                    Node n = server.redirect(response);

                    MessageHandler redirect = new MessageHandler(response, n.getNodeIp(), n.getNodePort(), this);

                    redirect.connectToServer();
                    redirect.sendMessage();
                    redirect.receiveResponse();
                }
                break;
            case SIGNUP:
                body = response.getBody().split(" ");
                System.out.println("REQUEST ID: " + Integer.remainderUnsigned(response.getSenderId().intValue(), 128));
                if (server.isResponsibleFor(response.getSenderId())) {
                    System.out.println("I'm the RESPONSIBLE server");
                    Message message = server.addUser(body[0], body[1]);
                    message.setInitialServerAddress(server.getNodeIp());
                    message.setInitialServerPort(server.getNodePort());
                    return message;
                } else {
                    System.out.println("REDIRECTING ID: " + Integer.remainderUnsigned(response.getSenderId().intValue(), 128));
                    Node n = server.redirect(response);

                    MessageHandler redirect = new MessageHandler(response, n.getNodeIp(), n.getNodePort(), this);

                    redirect.connectToServer();
                    redirect.sendMessage();
                    redirect.receiveResponse();
                }
                break;
            case CREATE_CHAT:
                if (server.isResponsibleFor(response.getSenderId()))
                    server.createChat((Chat) response.getObject());
                else {
                    System.out.println("REDIRECTING ID: " + response.getSenderId().intValue());
                    server.redirect(response);
                }
                break;
            case NEWNODE:
                body = response.getBody().split(" ");
                server.newNode(body);
                server.getDht().printFingerTable();
                break;
            case PREDECESSOR:
                Node temp = (Node) response.getObject();
                server.getDht().setPredecessor(temp);
                server.sendFingerTableToSuccessor();
                server.getDht().printFingerTable();
                break;
            case SUCCESSOR_FT:
                ArrayList<Node> ft = (ArrayList<Node>) response.getObject();
                server.getDht().updateFingerTableFromSuccessor(ft);
                server.getDht().setPredecessor(ft.get(0));
                server.getDht().printFingerTable();
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
            server.saveConnection(sslSocket, responseMessage.getSenderId());
            System.out.println("Mandando...");
            sendMessage(responseMessage);
        } catch (IOException e) {
            System.out.println("Error reading message...");
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading message...");
        }

    }
}