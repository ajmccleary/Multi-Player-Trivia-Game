package game;

import java.io.PrintWriter;
import java.net.*;

public class GameClient {
    //static variables
    private static DatagramSocket socket; //client socket
    private static int portNum;

    //instance variables
    private ClientWindow window;

    //constructor method
    public GameClient () {
        //connect to server
        Socket socket = new Socket(8989);

        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        writer.println();
        
        // Establishes a connection
       socket.connect(new InetSocketAddress("127.0.0.1", 8001));
        
       
    }

    public static void main (String args[]) {
        GameClient gc = new GameClient();
    }
}
