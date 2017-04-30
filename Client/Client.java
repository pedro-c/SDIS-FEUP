package Client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class Client {

    private Scanner scannerIn;
    private SSLSocket sslSocket;
    private SSLSocketFactory sslSocketFactory;
    private BufferedReader in;
    private PrintWriter out;

    public static void main(String[] args){
        Client client = new Client();
        client.mainMenu();
    }

    public Client(){
        scannerIn = new Scanner(System.in);
    }

    public void mainMenu() {
        String menu = "\n Menu " + "\n 1. Sign in" + "\n 2. Sign up" + "\n 3. Exit" + "\n";
        System.out.println(menu);
        int option = scannerIn.nextInt();
        switch (option){
            case 1:
                signInUser();
                break;
            case 2:
                signUpUser();
                break;
            case 3:
                System.exit(0);
        }

    }

    public void signInUser(){
        connectToServer();
        out.println("SignIn");
        closeSocket();
    }

    public void signUpUser(){
        connectToServer();
        out.println("SignUp");
        closeSocket();
    }

    public void connectToServer(){

        try {
            sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sslSocket = (SSLSocket) sslSocketFactory.createSocket("localhost",4445);

            sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
            in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
            out = new PrintWriter(sslSocket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error creating ssl socket...");
            e.printStackTrace();
        }
    }

    public void closeSocket(){
        try {
            sslSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing ssl socket...");
            e.printStackTrace();
        }
    }
}
