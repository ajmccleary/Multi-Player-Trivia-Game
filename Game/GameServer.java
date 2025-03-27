package game;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;

public class GameServer {
    //static variables
    private static DatagramSocket socket; //server socket
    private static int portNum;
    private static HashMap<String,Node> clients = new HashMap<String, Node>();
	private static File inFile = new File("ipConfig.txt");
    private static String nextLine;
    private static ExecutorService executorService;
    
    //instance variables
    
    //constructor method
    public GameServer() {        
        try (Scanner fileInput = new Scanner(inFile)) { //initialize scanner
            //munch first line
            fileInput.nextLine();

            //scan through file
            do {
                //store next line input
                nextLine = fileInput.nextLine();

                //parse input and store in new node
                Node newNode = new Node(nextLine.split(" ")[0], Integer.parseInt(nextLine.split(" ")[1]));
                
                //add newly created client to hashMap
                clients.put(newNode.getID(), newNode);

            } while (fileInput.hasNextLine());
        } catch (FileNotFoundException e) { //catch potential error thrown by scanner
			System.out.println("ipConfig.txt not found in root directory. Exiting program");
            System.exit(1);
        }

        //connect to specified socket
        try {
            GameServer.socket = new DatagramSocket(GameServer.portNum);
        } catch (SocketException e) {
            System.out.println("Failed to create socket. Reason " + e.getMessage());
            System.exit(1);
        }
    }

    //send question thread method (uses TCP)
    public void sendQuestion(Node client) {
        //blast question and question data to all clients on network
    }

    //detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        executorService.submit(() -> sendQuestion(clients.get("received ip:portnum")));
    }

    //main
    public static void main (String args[]) {
        GameServer gs = new GameServer();

        executorService = Executors.newFixedThreadPool(10);

        
    }
}
