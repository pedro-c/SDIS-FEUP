package Server;


import static Utilities.Utilities.createHash;
import static Utilities.Utilities.get32bitHashValue;

public class Node {

    protected final int nodeId;
    protected final String nodeIp;
    protected final String nodePort;

    public Node(String ip, String port){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeId = setNodeIdentifier();
    }

    /**
     * @return Returns 32-bit hash using serv
     * er ip and server port
     */
    public int setNodeIdentifier() {
        return get32bitHashValue(createHash(nodeIp +nodePort));
    }

    /**
     * @return Returns node identifier, 32-bit int hash
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     *
     * @return Returns node ip address
     */
    public String getNodeIp() {
        return nodeIp;
    }

    /**
     * Get node port
     * @return node port
     */
    public String getNodePort() {
        return nodePort;
    }
}
