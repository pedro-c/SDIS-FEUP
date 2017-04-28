package Server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;

public class Server {

    private SSLSocket sslSocket;
    private SSLServerSocket sslServerSocket;
    private SSLSocketFactory sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    private int port;
    private String host;

    public Server(int port, String host){
        this.port = port;
        this.host = host;

        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
        } catch (IOException e) {
            System.out.println("Failed to create sslServerSocket");
            e.printStackTrace();
        }
        listen();
    }

    public void listen(){
        while(true){
            try {
                new ConnectionHandler((SSLSocket)sslServerSocket.accept());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ConnectionHandler implements Runnable{

        private SSLSocket sslSocket;

        public ConnectionHandler(SSLSocket socket){
            sslSocket = socket;
        }

        public void run(){

        }
    }


    public static void main(String[] args) {
        //For now lets receive a port
        if(args.length != 1){
            throw new IllegalArgumentException("\\nUsage: java server.ServerLauncher<port> <hostname>");
        }

        int port = Integer.parseInt(args[0]);
        String hostname = args[1];

        Server server= new Server(port,hostname);
    }
}
