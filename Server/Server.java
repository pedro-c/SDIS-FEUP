package Server;

public class Server {

    private int port;

    public Server(int port){
        this.port = port;

        //defining channels

        //start listening

        //process response

        //message handler
    }

    public static void main(String[] args) {
        //For now lets receive a port
        if(args.length != 1){
            throw new IllegalArgumentException("\\nUsage: java server.ServerLauncher<port>*");
        }

        int port = Integer.parseInt(args[0]);

        Server server= new Server(port);
    }
}
