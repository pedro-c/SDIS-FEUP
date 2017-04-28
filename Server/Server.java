package Server;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

public class Server {

    private SSLSocket sslSocket;
    private SSLServerSocket sslServerSocket;
    private SSLSocketFactory sslSocketFactory;
    private SSLServerSocketFactory sslServerSocketFactory;
    private Hashtable<Integer,String[]> serverConfig;
    private int serverId;
    private int serverPort;

    public Server(int serverId){
        this.serverId = serverId;
        this.serverConfig = new Hashtable<>();

        readFile();
        serverPort =Integer.parseInt(serverConfig.get(this.serverId)[1]);


        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(serverPort);
        } catch (IOException e) {
            System.out.println("Failed to create sslServerSocket");
            e.printStackTrace();
        }
        //synchronize
        listen();
    }

    public void readFile(){

        try(BufferedReader reader = new BufferedReader(new FileReader(".config"))) {
            String line = reader.readLine();

            int serverId = 0;

            while (line != null) {

                String[] serverInfo = line.split(":");
                serverConfig.put(serverId,serverInfo);
                line = reader.readLine();
            }
        }
        catch (IOException e){

        }

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
            this.sslSocket = socket;
        }

        public void run(){

        }
    }


    public static void main(String[] args) {
        //For now lets receive a port and a hostname
        if(args.length != 1){
            throw new IllegalArgumentException("\\nUsage: java server.ServerLauncher<id>");
        }

        int id = Integer.parseInt(args[0]);
        Server server= new Server(id);
    }
}
