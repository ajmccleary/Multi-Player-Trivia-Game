package game;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class GameServer {
    //static variables
    private static DatagramSocket socket; //server socket
    private static int portNum;
    private static HashMap<String,Node> clients = new HashMap<String, Node>();
	private static File inFile;
    private static String nextLine;
    private static ExecutorService executorService;
    
    //instance variables
    private int questionNumber = 0;


    //constructor method
    public GameServer() {
        //declare inFile
        inFile = new File("ipConfig.txt");
        
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
    public void sendQuestions (Node client) {
        //while thread not shut down
        //blast question and question data to all clients on network
    }

    //detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        //receive prcoess store data from client attempting to connect

        //create a new thread with the client's information to send questions to client
        executorService.submit(() -> sendQuestions(clients.get("received ip:portnum")));
    }

    //monitor buzzer from all clients (UDP)
    public void buzzerHandler() {
        //while question is not next

    }

    //main
    public static void main (String args[]) {
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


        GameServer gs = new GameServer();

        executorService = Executors.newFixedThreadPool(10);

        executorService.submit(() -> gs.listenThread());

        //gameplay loop
        while (gs.questionNumber <= 20) {
            //game logic

            gs.questionNumber++;
        }
    }
}
