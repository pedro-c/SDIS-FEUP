package Server;

import Messages.Message;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Utilities.Constants.*;
import static Utilities.Utilities.createHash;

public class Server extends Node {

    /**
     * Key is the user id (32-bit hash from e-mail) and value is the 256-bit hashed user password
     */
    private Hashtable<byte[], byte[]> users;
    /**
     * Key is an integer representing the m nodes and the value it's the server identifier
     * (32-bit integer hash from ip+port)
     */
    private HashMap<Integer, Node> fingerTable = new HashMap<>();
    private SSLServerSocket sslServerSocket;
    private SSLServerSocketFactory sslServerSocketFactory;
    private ExecutorService poolThread = Executors.newFixedThreadPool(MAX_NUMBER_OF_REQUESTS);
    private ObjectInputStream serverInputStream;
    private ObjectOutputStream serverOutputStream;
    private Node predecessor = this;

    public Server(String args[]) {
        super(args[0], args[1]);

        initFingerTable();
        saveServerInfoToDisk();
        loadServersInfoFromDisk();
        initServerSocket();
        joinNetwork();

    }

    public static void main(String[] args) {
        //For now lets receive a port and a hostname
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java server.Server<ip,port>");
        }
        Server server = new Server(args);
        server.listen();
    }

    /**
     * Listens for incoming connection requests
     */
    public void listen() {
        while (true) {
            try {
                System.out.println("Listening...");
                ConnectionHandler handler = new ConnectionHandler((SSLSocket) sslServerSocket.accept());
                poolThread.submit(handler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Registers server to RMI
     */
    public void registerServer() {

        //TODO: Do we really need RMI? It's not scalable on the internet
        /*
        try {
            Registry registry = LocateRegistry.getRegistry();
            try{
                registry.lookup(this.serverId.toString());
                registerServer();
            }
            catch (NotBoundException e) {
                try {
                    registry.rebind(serverId.toString(), this);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    System.out.println("Failed to bind peer to registry");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to find registry");
        }*/
    }

    /**
     * Initiates the server socket for incoming requests
     */
    public void initServerSocket() {
        sslServerSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try {
            sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(Integer.parseInt(this.getNodePort()));
            // TODO: Not working
            // sslServerSocket.setNeedClientAuth(true);
            sslServerSocket.setEnabledCipherSuites(sslServerSocket.getSupportedCipherSuites());

        } catch (IOException e) {
            System.out.println("Failed to create sslServerSocket");
            e.printStackTrace();
        }
    }

    /**
     * Initializes the finger table with m values (max number of nodes = 2^m)
     */
    public void initFingerTable() {
        for (int i = 1; i <= MAX_FINGER_TABLE_SIZE; i++) {
            fingerTable.put(i, this);
        }
    }

    /**
     * Sends a message to the network
     */
    public void joinNetwork() {

        Message message = new Message(NEWNODE, Integer.toString(this.getNodeId()), Integer.toString(predecessor.getNodeId()), predecessor.getNodeIp(), predecessor.getNodePort());

        //TODO: send message NEWNODE, receive message and responde with return from serverLookUp()

    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public int serverLookUp(int key) {
        int id=this.getNodeId();

        for (Map.Entry<Integer, Node> entry : fingerTable.entrySet()) {
            id=entry.getValue().getNodeId();
            if (id > key) {
                return id;
            }
        }
        return id;
    }

    /**
     * Checks if .config file already has info about this server, if not appends ip:port:id
     */
    public void saveServerInfoToDisk() {
        try {
            File file = new File("./", ".config");

            if (!file.isFile() && !file.createNewFile()) {
                throw new IOException("Error creating new file: " + file.getAbsolutePath());
            }

            BufferedReader reader = new BufferedReader(new FileReader(".config"));
            String line = reader.readLine();
            while (line != null) {
                String[] serverInfo = line.split(":");
                System.out.println(this.getNodeId());
                System.out.println(serverInfo[2]);
                if (serverInfo[2].equals(Integer.toString(this.getNodeId()))) {
                    return;
                }
                line = reader.readLine();
            }
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(".config", true)));
            out.println(this.getNodeIp() + ":" + this.getNodePort() + ":" + this.getNodeId());
            out.close();
            System.out.println("Saved server info to config file");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets servers info from .config file and loads the finger table with closest preceding servers
     */
    public void loadServersInfoFromDisk() {

        try (BufferedReader reader = new BufferedReader(new FileReader(".config"))) {
            String line = reader.readLine();

            while (line != null) {
                String[] nodeInfo = line.split(":");
                String nodeId = nodeInfo[2];
                String nodeIp = nodeInfo[0];
                String nodePort = nodeInfo[1];
                if (!nodeIp.equals(this.getNodeIp())) {
                    int id = Integer.parseInt(nodeId);
                    for (Map.Entry<Integer, Node> entry : fingerTable.entrySet()) {
                        /**
                         * successor formula = succ(serverId+2^(i-1))
                         *
                         * successor is a possible node responsible for the values between
                         * the current and the successor.
                         *
                         * serverId equals to this node position in the circle
                         */
                        int succ = (int) (this.getNodeId() + Math.pow(2, (entry.getKey() - 1)));
                        /**
                         * if successor number is bigger than the circle size (max number of nodes)
                         * it starts counting from the beginning
                         * by removing this node position (serverId) from formula
                         */
                        if (succ > Math.pow(2, MAX_FINGER_TABLE_SIZE)) {
                            succ = (int) (Math.pow(2, (entry.getKey() - 1)));
                        }
                        /**
                         * if the successor is smaller than the value of the node we are readingee
                         * from the config file this means that the node we are reading might be
                         * responsible for the keys in between.
                         * If there isn't another node responsible
                         * for this interval or the node we are reading has a smaller value
                         * than the node that used to be responsible for this interval,
                         * than the node we are reading is now the node responsible
                         */
                        if (succ < id) {
                            if (entry.getValue() == null) {
                                fingerTable.put(entry.getKey(), new Node(nodeIp,nodePort));
                            } else if (id < entry.getValue().getNodeId()) {
                                fingerTable.put(entry.getKey(), new Node(nodeIp,nodePort));
                            }
                        }
                    }
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void analyseResponse(Message response) {
        String[] body = response.getBody().split(" ");

        switch (response.getMessageType()){
            case SIGNIN:
                loginUser(body[0],body[1]);
                break;
            case SIGNUP:
                registUser(body[0],body[1]);
                break;
            default:
                break;
        }
    }

    /**
     * Regists user
     *
     * @param email    user email
     * @param password user password
     */
    public void registUser(String email, String password){


        byte[] user_email = createHash(email);
        System.out.println("Sign in with  " + user_email);

      /* if (users.containsKey(user_email))
            System.out.println("Email already exists. Try to sign in instead of sign up...");
       else {
           users.put(user_email,createHash(password));
           System.out.println("Signed up with success!");
       }*/

    }

    /**
     * Authenticates user already registered
     *
     * @param email    user email
     * @param password user password
     * @return true if user authentication went well, false if don't
     */
    public boolean loginUser(String email, String password) {

        System.out.println("Sign up with " + email);

       /* if (!users.containsKey(email)) {
            System.out.println("Try to create an account. Your email was not found on the database...");
            return false;
        }

        if (!users.get(email).equals(createHash(password))) {
            System.out.println("Impossible to sign in, wrong email or password...");
            return false;
        }

        System.out.println("Logged in with success!");
        */
        return true;
    }

    /**
     * Handles new SSL Connections to the server
     */
    public class ConnectionHandler implements Runnable {

        private SSLSocket sslSocket;
        private BufferedReader in;

        public ConnectionHandler(SSLSocket socket) {
            this.sslSocket = socket;
            try {

                 in = new BufferedReader(new InputStreamReader(sslSocket.getInputStream()));
                serverOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
                serverInputStream = new ObjectInputStream(sslSocket.getInputStream());
            } catch (IOException e) {
                System.out.println("Error creating buffered reader...");
                e.printStackTrace();
            }
        }

        public void run() {
            Message message = null;
            try {
                message = (Message) serverInputStream.readObject();
                analyseResponse(message);
            } catch (IOException e) {
                System.out.println("Error reading message...");
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                System.out.println("Error reading message...");
                e.printStackTrace();
            }

         }
        }
    }

