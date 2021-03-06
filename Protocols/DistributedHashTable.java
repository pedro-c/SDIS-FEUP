package Protocols;


import Server.Node;
import Server.Server;

import java.io.Serializable;
import java.util.ArrayList;

import static Utilities.Constants.MAX_FINGER_TABLE_SIZE;
import static Utilities.Constants.MAX_NUMBER_OF_NODES;

public class DistributedHashTable implements Serializable {

    private ArrayList<Node> fingerTable = new ArrayList<Node>();
    private Node predecessor;
    private Server server;

    public DistributedHashTable(Server server) {
        this.server = server;
        this.predecessor = server;
        initFingerTable();
        printFingerTable();
    }


    /**
     * Initializes the finger table with m values (max number of nodes = 2^m)
     */
    public void initFingerTable() {
        for (int i = 0; i <= MAX_FINGER_TABLE_SIZE; i++) {
            fingerTable.add(server);
        }
    }

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
   /* public Node nodeLookUp(int key) {

        key = Integer.remainderUnsigned(key, 128);

        long distance, position;
        Node successor = server;
        long previousId = server.getNodeId();
        for (int i = 1; i < fingerTable.size(); i++) {
            Node node = fingerTable.get(i);

            distance = (long) Math.pow(2, (double) i - 1);
            position = server.getNodeId() + distance;
            if (key > server.getNodeId()) {
                if (node.getNodeId() > key) {
                    successor = node;
                    break;
                }
            } else {
                if (node.getNodeId() < previousId) {
                    if (key < node.getNodeId()) {
                        successor = node;
                    }
                    previousId = node.getNodeId();
                }
            }
        }

        if (successor == server && key > server.getNodeId()) {

            for (int i = 2; i < fingerTable.size(); i++) {
                Node tempNode1 = fingerTable.get(i-1);
                Node tempNode2 = fingerTable.get(i);

                if (tempNode1.getNodeId() > tempNode2.getNodeId()) {
                    successor = tempNode2;
                    break;
                }

                if(tempNode1.getNodeId() == tempNode2.getNodeId() && tempNode1.getNodeId() != server.getNodeId()){
                    successor = tempNode1;
                }
            }

        }

        System.out.println("Successor of " + key + " : " + successor.getNodeId());
        return successor;
    }*/

    /**
     * Looks up in the finger table which server has the closest smallest key comparing to the key we want to lookup
     *
     * @param key 256-bit identifier
     */
    public Node nodeLookUp(int key) {

        int goesAround = fingerTable.get(MAX_FINGER_TABLE_SIZE).getNodeId() - server.getNodeId();
        int keyGoesAround = key - server.getNodeId();

        Node successor = server;

        if (goesAround <= 0) { //Goes around
            if (keyGoesAround <= 0) {
                for (int i = 1; i < fingerTable.size(); i++) {
                    Node tempNode1 = fingerTable.get(i);

                    if (tempNode1.getNodeId() > server.getNodeId())
                        continue;

                    if (key < tempNode1.getNodeId()) {
                        successor = tempNode1;
                        break;
                    }
                }
            } else {
                if ((fingerTable.get(1).getNodeId() - server.getNodeId()) < 0) {
                    successor = fingerTable.get(1);
                } else {
                    for (int i = 1; i < fingerTable.size() - 1; i++) {
                        Node tempNode1 = fingerTable.get(i);
                        Node tempNode2 = fingerTable.get(i + 1);
                        if (key < tempNode1.getNodeId()) {
                            successor = tempNode1;
                            break;
                        }

                        if (tempNode2.getNodeId() < tempNode1.getNodeId()) {
                            successor = tempNode2;
                            break;
                        }
                    }
                }
            }
        } else { //Don't go around
            for (int i = 7; i > 1; i--) {
                Node tempNode1 = fingerTable.get(i);
                Node tempNode2 = fingerTable.get(i - 1);

                if (key < tempNode1.getNodeId() && key > tempNode2.getNodeId()) {
                    successor = tempNode1;
                    break;
                }
                if (key < tempNode1.getNodeId())
                    successor = tempNode1;
            }
        }

        System.out.println("Successor of " + key + " : " + successor.getNodeId());
        return successor;
    }

    /*
    public Node lookUp(int key) {
        int goesAround = fingerTable.get(MAX_FINGER_TABLE_SIZE).getNodeId() - server.getNodeId();
        int keyGoesAround = key - server.getNodeId();

        Node successor = server;

        if(goesAround < 0){ //Goes around
            if(keyGoesAround < 0){
                for (int i = 1; i < fingerTable.size(); i++) {
                    Node tempNode1 = fingerTable.get(i);

                    if(tempNode1.getNodeId() > server.getNodeId())
                        continue;

                    if(key < tempNode1.getNodeId()){
                        successor = tempNode1;
                        break;
                    }
                }
            }
            else {
                if((fingerTable.get(1).getNodeId() - server.getNodeId()) < 0){
                    successor = fingerTable.get(1);
                }
                else {
                    for (int i = 1; i < fingerTable.size() - 1; i++) {
                        Node tempNode1 = fingerTable.get(i);
                        Node tempNode2 = fingerTable.get(i+1);

                        if(key < tempNode1.getNodeId()){
                            successor = tempNode1;
                            break;
                        }

                        if(tempNode2.getNodeId() <= tempNode1.getNodeId()){
                            successor = tempNode2;
                            break;
                        }
                    }
                }
            }
        }
        else { //Don't go around
            for (int i = 7; i > 1; i++) {
                Node tempNode1 = fingerTable.get(i);
                Node tempNode2 = fingerTable.get(i-1);

                if(key < tempNode1.getNodeId() && key > tempNode2.getNodeId()){
                    successor = tempNode1;
                    break;
                }
                if(key < tempNode1.getNodeId())
                    successor = tempNode1;
            }
        }

        return successor;
    }
*/
    public void removeNode(int nodeId) {
        ArrayList<Node> oldFT = new ArrayList<Node>();

        for (int i = 0; i <= MAX_FINGER_TABLE_SIZE; i++) {
            oldFT.add(fingerTable.get(i));

        }
        fingerTable.clear();

        initFingerTable();

        System.out.println("Old finger table:");
        printFingerTable();

        for (int i = 1; i <= MAX_FINGER_TABLE_SIZE; i++) {
            if (oldFT.get(i).getNodeId() != nodeId) {
                updateFingerTable(oldFT.get(i));
            }
        }
        if (predecessor.getNodeId() != nodeId) {
            updateFingerTable(predecessor);
        } else {
            predecessor = server;
        }


        System.out.println("New finger table:");
        printFingerTable();

    }

    /**
     * This functions updates the server finger table with the new node info
     *
     * @param newNode new node on the distributed hash table
     */
    public void updateFingerTable(Node newNode) {

        long position;
        long distance;

        for (int i = 1; i < fingerTable.size(); i++) {
            Node node = fingerTable.get(i);

            distance = (long) Math.pow(2, (double) i - 1);
            position = server.getNodeId() + distance;
            if (node.getNodeId() == server.getNodeId() && newNode.getNodeId() >= position) {
                fingerTable.set(i, newNode);
            } else if (newNode.getNodeId() >= position && newNode.getNodeId() < node.getNodeId()) {
                fingerTable.set(i, newNode);
            } else if (newNode.getNodeId() < server.getNodeId()) {
                if (newNode.getNodeId() < node.getNodeId()) {
                    if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && MAX_NUMBER_OF_NODES - server.getNodeId() + node.getNodeId() > MAX_NUMBER_OF_NODES - server.getNodeId() + newNode.getNodeId()) {
                        if (node.getNodeId() < server.getNodeId() || node.getNodeId() == server.getNodeId()) {
                            fingerTable.set(i, newNode);
                            System.out.println("3");
                        }

                    } else if (MAX_NUMBER_OF_NODES - position + newNode.getNodeId() >= 0 && node.getNodeId() == server.getNodeId()) {
                        fingerTable.set(i, newNode);
                        System.out.println("4");
                    }
                }
            } else if (newNode.getNodeId() > server.getNodeId() && newNode.getNodeId() >= position && node.getNodeId() < position) {
                if (MAX_NUMBER_OF_NODES - server.getNodeId() + node.getNodeId() < MAX_NUMBER_OF_NODES - server.getNodeId() + newNode.getNodeId()) {
                    fingerTable.set(i, newNode);
                }
            }
        }
    }

    /**
     * Receives the successor finger table and updates is own finger table
     *
     * @param successorFingerTable successor finger table
     */
    public void updateFingerTableFromSuccessor(ArrayList<Node> successorFingerTable) {

        System.out.println(successorFingerTable.size());
        for (int i = 0; i < successorFingerTable.size(); i++) {
            updateFingerTable(successorFingerTable.get(i));
        }

        printFingerTable();
    }

    public Node fingerTableNode(int id) {
        return fingerTable.get(id);
    }

    public Node getPredecessor() {
        return predecessor;
    }

    public void setPredecessor(Node node) {

        if (getPredecessor().getNodeId() != node.getNodeId()) {
            updateFingerTable(node);
            predecessor = node;
            System.out.println("New predecessor: " + node.getNodeId());
        }
    }

    public void printFingerTable() {
        System.out.println("FINGERTABLE");
        System.out.println("-----------");
        System.out.println("Node ID: " + server.getNodeId());
        System.out.println("Predecessor: " + getPredecessor().getNodeId());
        System.out.println("-----------");
        System.out.println("FINGERtableSize: " + fingerTable.size());
        for (int i = 1; i < fingerTable.size(); i++) {
            System.out.println(i + "    " + fingerTable.get(i).getNodeId());
        }
        System.out.println("-----------");
    }

    public ArrayList<Node> getFingerTable() {
        return fingerTable;
    }

    public Node getSuccessor() {

        Node successor = null;

        for (Node node : fingerTable) {
            if (node.getNodeId() != server.getNode().getNodeId()) {
                successor = node;
                break;
            }
        }

        return successor;
    }

}
