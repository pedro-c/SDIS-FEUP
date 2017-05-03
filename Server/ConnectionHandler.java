package Server;

import Messages.Message;

import javax.net.ssl.SSLSocket;
import java.io.*;

import static Utilities.Constants.SIGNIN;
import static Utilities.Constants.SIGNUP;

/**
 * Handles new SSL Connections to the server
 */
public class ConnectionHandler implements Runnable {

    private SSLSocket sslSocket;
    private BufferedReader in;
    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Server server;

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


    public void analyseResponse(Message response) {
        String[] body = response.getBody().split(" ");

        System.out.println(response.getMessageType());

        switch (response.getMessageType()) {
            case SIGNIN:
                server.loginUser(body[0], body[1]);
                break;
            case SIGNUP:
                server.addUser(body[0],body[1]);
                break;
            default:
                break;
        }
    }


    public void run() {

        Message message = null;
        try {
            message = (Message) serverInputStream.readObject();
            analyseResponse(message);
        } catch (IOException e) {
            System.out.println("Error reading message...");
        } catch (ClassNotFoundException e) {
            System.out.println("Error reading message...");
        }

    }
}