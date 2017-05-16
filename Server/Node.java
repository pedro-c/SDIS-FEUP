package Server;


import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static Utilities.Utilities.createHash;
import static Utilities.Utilities.get32bitHashValue;

public class Node implements Serializable {

    protected int nodeId;
    protected String nodeIp;
    protected int nodePort;

    public Node(String ip, int port) {
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeId = setNodeIdentifier();
    }

    public Node(String ip, int port, int key) {
        this.nodeIp = ip;
        this.nodePort = port;
        this.nodeId = key;
    }

    public Node() {

    }

    /**
     * @return Returns 32-bit hash using serv
     * er ip and server port
     */
    public int setNodeIdentifier() {
        return Math.abs(get32bitHashValue(createHash(nodeIp+ Integer.toString(nodePort))));
    }

    /**
     * @return Returns node identifier, 32-bit int hash
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @return Returns node ip address
     */
    public String getNodeIp() {
        return nodeIp;
    }

    /**
     * Get node port
     *
     * @return node port
     */
    public int getNodePort() {
        return nodePort;
    }

    public Node getNode() {
        return this;
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

        if (nodeId == ((Node) o).getNodeId())
            return true;

        return false;
    }

}
