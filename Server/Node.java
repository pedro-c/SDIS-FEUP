package Server;


import static Utilities.Utilities.createHash;
import static Utilities.Utilities.get32bitHashValue;

public class Node {

    private int nodeId;
    private String nodeIp;
    private String nodePort;

    public Node(String ip, String port){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeId = getServerIdentifier();
    }

    /**
     * @return Returns 32-bit hash using server ip and server port
     */
    public int getServerIdentifier() {
        return get32bitHashValue(createHash(nodeIp +nodePort));
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public String getNodePort() {
        return nodePort;
    }

    public void setNodePort(String nodePort) {
        this.nodePort = nodePort;
    }
}
