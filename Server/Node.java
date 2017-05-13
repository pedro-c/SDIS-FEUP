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
        this.nodeId = setNodeIdentifier();
    }

    public Node(String ip, String port, int key){
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeId = key;
    }

    public Node(){

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
     * Set node ip address
     * @param nodeIp Ip address
     */
    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    /**
     * Get node port
     * @return node port
     */
    public String getNodePort() {
        return nodePort;
    }

    /**
     * Set node port
     * @param nodePort node port
     */
    public void setNodePort(String nodePort) {
        this.nodePort = nodePort;
    }

}
