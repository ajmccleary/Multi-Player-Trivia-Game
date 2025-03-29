package game;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    //static variables
    private static GameServer gs;
    private static int portNum;
    private static DatagramSocket buzzerSocket; //UDP buzzer socket
    private static ServerSocket gameSocket;
	private static File inFile;
    private static String nextLine;
    
    //instance variables
    private ExecutorService executorService;
    private Queue<Node> buzzerQueue = new LinkedList<Node>();
    private HashMap<String,Node> clients = new HashMap<String, Node>();
    private int questionNumber = 0;
    private boolean shutdownFlag = false;
    
    
    //constructor method
    public GameServer() {
        //declare inFile
        inFile = new File("ipConfig.txt");
        
        try (Scanner fileInput = new Scanner(inFile)) { //initialize scanner
            //read first line
            nextLine = fileInput.nextLine();

            //store port number
            GameServer.portNum = Integer.parseInt(nextLine.split(" ")[1]);

            //scan through file
            do {
                //store next line input
                nextLine = fileInput.nextLine();

                //parse input and store in new node
                Node newNode = new Node(nextLine.split(" ")[0], Integer.parseInt(nextLine.split(" ")[1]));
                
                //add newly created client to hashMap
                this.clients.put(newNode.getID(), newNode);

            } while (fileInput.hasNextLine());
        } catch (FileNotFoundException e) { //catch potential error thrown by scanner
			System.out.println("ipConfig.txt not found in root directory. Exiting program");
            System.exit(1);
        }

        //connect to specified socket for UDP and TCP connections
        try {
            //port num 8000 for buzzer
            GameServer.buzzerSocket = new DatagramSocket(GameServer.portNum - 1);

            //port num 8001 for game
            GameServer.gameSocket = new ServerSocket(GameServer.portNum + 1);
        } catch (SocketException e) {
            System.out.println("Failed to create UDP socket. Reason " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Failed to create TCP socket. Reason " + e.getMessage());
            System.exit(1);
        }
    }

    //send question thread method (uses TCP)
    public void sendQuestions (Node client) {
        //while thread not shut down
        //blast question and question data to all clients on network

        //buzzer socket test, code beneath is temporary
        DatagramSocket test;
        try {
            test = new DatagramSocket(8002);
            DatagramPacket buzz = new DatagramPacket(new byte[1024], 1024, InetAddress.getByName("127.0.0.1"), portNum - 1);
            test.send(buzz);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        }
    }

    //detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        //receive prcoess store data from client attempting to connect

        //create a new thread with the client's information to send questions to client
        gs.executorService.submit(() -> gs.sendQuestions(gs.clients.get("127.0.0.1:8000")));
    }

    //monitor buzzer from all clients (UDP)
    public void buzzerHandler() {
        while (!shutdownFlag) {
            //initialize reply packet
            byte[] replyData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(replyData, replyData.length);

            //use UDP socket
            try {
                buzzerSocket.receive(replyPacket);
            } catch (IOException e) {
                System.out.println("Failed to connect socket. Reason " + e.getMessage());
            }

            //add replies to queue in order received
            gs.buzzerQueue.add(clients.get(replyPacket.getAddress().getHostAddress() + ":" + replyPacket.getPort()));

            //pop from queue to pick player to answer
            System.out.println(gs.buzzerQueue.remove().getID()); //printing rn change to logic to let player answer question
        }
    }

    //main
    public static void main (String args[]) { //hi zak (and not fahim) - jjguy
        System.out.println("Game Server Starting...");

        // Getting size of config file
        File configFile = new File("questions.txt");
        int size = 0;
        try (Scanner fileScan = new Scanner(configFile)) {
            while (fileScan.hasNextLine()) {
                size++;
                fileScan.nextLine();
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }

        // Create an array to hold the questions
        String[] questions = new String[size]; 
        // String[] options = new String[4];

        List<String> options = new ArrayList<>();

        int index = 0;
        File questionFile = new File("questions.txt");
        try (Scanner fileScan = new Scanner(questionFile)) {
            while (fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" \\| ");

                questions[index] = parts[0];
                for (int i = 1; i <= 4; i++) {
                    // options[i - 1] = parts[i];

                    options.add(parts[i]);
                }
                index++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }

        for (int i = 0; i < questions.length; i++) {
            System.out.println("Question " + (i + 1) + ": " + questions[i]);
            for (int j = 0; j < 4; j++) {
                System.out.println("Option " + (j + 1) + ": " + options.get(j + (i * 4)));
            }
            System.out.println();
        }

        GameServer.gs = new GameServer();

        gs.executorService = Executors.newFixedThreadPool(5);
        gs.executorService.submit(() -> gs.buzzerHandler());

        System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        gs.executorService.submit(() -> gs.listenThread());
        
        
        //gameplay loop
        while (gs.questionNumber <= 20) {
            //game logic

            //if question has progressed, increment
            //base on timer
            //gs.questionNumber++;
        }

        //if questions complete
        gs.shutdownFlag = true;
    }
}
