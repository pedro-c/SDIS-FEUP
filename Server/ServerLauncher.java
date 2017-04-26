package Server;

public class ServerLauncher {

    public static void main(String[] args) {
        //For now lets receive a port
        if(args.length != 1){
            throw new IllegalArgumentException("\\nUsage: java server.ServerLauncher<port>*");
        }

        int port = Integer.parseInt(args[0]);

        Server server= new Server(port);
    }
}
