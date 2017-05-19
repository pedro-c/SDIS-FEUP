package Server;

import Chat.Chat;
import Messages.Message;
import Messages.MessageHandler;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

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

        try {
            if (message == null)
                throw new IOException();
            serverOutputStream.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("CH Sending message with type: " + message.getMessageType() + " and body " + message.getBody());
    }


    /**
     * Analyses Responses
     *
     * @param response
     */
    public void analyseResponse(Message response) {
        String[] body;
        System.out.println(response.getMessageType());

        System.out.println("CH receiving message: " + response.getMessageType());

        switch (response.getMessageType()) {
            case SIGNIN:
                isResponsible(response);
                break;
            case SIGNUP:
                isResponsible(response);
                break;
            case CREATE_CHAT:
                if (server.isResponsibleFor(response.getSenderId())) {
                    System.out.println("I'm the RESPONSIBLE server");
                    sendMessage(server.createChat((Chat) response.getObject()));
                } else {
                    redirect(response);
                }
                break;
            case INVITE_USER:
                if (server.isResponsibleFor(response.getSenderId())) {
                    System.out.println("I'm the RESPONSIBLE server");
                    server.createParticipantChat((ServerChat) response.getObject());
                } else {
                    redirect(response);
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
            case BACKUP_USER:
                if (response.getObject() != null) {
                    User user = (User) response.getObject();
                    server.getBackups().put(createHash(user.getEmail()), user);
                    sendMessage(new Message(SERVER_SUCCESS, BigInteger.valueOf(this.server.getNodeId()), USER_ADDED));
                } else
                    sendMessage(server.backupInfo(response));
                break;
            case ADD_USER:
                sendMessage(server.addUser((User) response.getObject()));
                break;
            case USER_UPDATED_CONNECTION:
                server.saveConnection(this,response.getSenderId());
                break;
            default:
                break;
        }
    }

    public void closeConnection() {
        try {
            sslSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to close ssl connection");
        }
    }

    public void isResponsible(Message response) {
        String[] body = response.getBody().split(" ");
        System.out.println("REQUEST ID: " + Integer.remainderUnsigned(response.getSenderId().intValue(), 128));
        if (server.isResponsibleFor(response.getSenderId())) {
            System.out.println("I'm the RESPONSIBLE server");
            server.saveConnection(this, response.getSenderId());
            Message message = server.addUser(body[0], body[1]);
            message.setInitialServerAddress(server.getNodeIp());
            message.setInitialServerPort(server.getNodePort());
            System.out.println("Replying to client with: " + message.getMessageType());
            sendMessage(message);
        } else {
            redirect(response);
        }

    }

    public void redirect(Message response) {
        System.out.println("REDIRECTING ID: " + response.getSenderId().intValue());
        Node n = server.redirect(response);
        MessageHandler redirect = new MessageHandler(response, n.getNodeIp(), n.getNodePort(), this);
        redirect.connectToServer();
        redirect.sendMessage();
        redirect.receiveResponse();
    }

    /**
     * Reads Messages
     */
    public void run() {
        Message message = null;
        while(true){
            try {
                System.out.println("Reading...");
                message = (Message) serverInputStream.readObject();
                analyseResponse(message);
            } catch (IOException e) {
                System.out.println("Closed Connection");
                break;
            } catch (ClassNotFoundException e) {
                System.out.println("Closed Connection");
                break;
            }
        }
    }
}