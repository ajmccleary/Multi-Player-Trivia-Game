package game;

public class Node { //represent a client on the network
    //instance variables
    private String ipAddress;
    private int portNum;

    //constructor method
    public Node(String ipAddress, int portNum) {
        this.ipAddress = ipAddress;
        this.portNum = portNum;
    }

    /**
     * Getter for the stored unique id of the node.
     * 
     * @return The unique id of the node, formatted as <ipaddress>:<portnumber>.
     */
    public String getID() {
        return (this.ipAddress + ":" + portNum);
    }
}
