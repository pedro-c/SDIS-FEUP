package Server;


import static Utilities.Utilities.createHash;
import static Utilities.Utilities.get32bitHashValue;

public class Node {

    protected final int nodeId;
    protected final String nodeIp;
    protected final String nodePort;

    public Node(int id, String ip, String port){
        this.nodeIp = ip;
        this.nodePort = port;
        //this.nodeId = setNodeIdentifier();

        this.nodeId = id;
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
     * Get node port
     * @return node port
     */
    public String getNodePort() {
        return nodePort;
    }

    @Override
    public boolean equals(Object o) {

        if (o == null) {
            return false;
        }
        if (!Node.class.isAssignableFrom(o.getClass())) {
            return false;
        }

        final Node node = (Node) o;

        if(nodeId == ((Node) o).getNodeId())
            return true;

        return false;
    }

}
