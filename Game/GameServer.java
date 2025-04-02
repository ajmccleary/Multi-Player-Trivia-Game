package game;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    // static variables
    private static GameServer gs;
    private static int portNum = 8001;
    private static DatagramSocket buzzerSocket; // UDP buzzer socket
    private static ServerSocket gameSocket; // TCP game socket

    // instance variables
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<String> buzzerQueue = new ConcurrentLinkedQueue<String>();
    private HashMap<String, Integer> clients = new HashMap<String, Integer>();
    private int questionNumber = 0;
    private String[] questions;
    private boolean shutdownFlag = false;

    // constructor method
    public GameServer() {
        // connect to specified socket for UDP and TCP connections
        try {
            // port num 8000 for buzzer
            GameServer.buzzerSocket = new DatagramSocket(portNum - 1);

            // port num 8001 for game
            GameServer.gameSocket = new ServerSocket(portNum);

        } catch (SocketException e) {
            System.out.println("Failed to create UDP socket. Reason " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Failed to create TCP socket. Reason " + e.getMessage());
            System.exit(1);
        }
    }

    // send question thread method (uses TCP)
    public void clientThread (Socket clientSocket) {
            
            try(
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true );
            ){
                //Send each question and its options to the client
                for(int i = 0; i< questions.length; i++){
                    StringBuilder questionData = new StringBuilder(questions[i]);
                    for(int j = 0; j < 4; j++){
                        questionData.append(";").append(options.get(j + (i * 4)));
                        System.out.println("The fuck");
                    }
                    // Sends the questions and options
                    out.println(questionData.toString());
                    Thread.sleep(20000);    // wait 20 seconds before sending the next question
                }
            } catch (IOException | InterruptedException e){
                System.err.println("Error handling client: " + e.getMessage());
            } finally{
                synchronized (clientSockets){
                    clientSockets.remove(clientSocket);
                }
            }
            

        // //periodically blast question and question data to client
        // try (OutputStream out = clientSocket.getOutputStream()) {
        //     while (!gs.shutdownFlag) {
        //         out.write(1); //replace 1 with questions array somehow
        //     }
        // } catch (IOException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
    }

    // detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        while (!gs.shutdownFlag) {
            // receive prcoess and store data from client attempting to connect
            try {
                Socket clientSocket = gameSocket.accept();
                synchronized(clientSockets){
                    clientSockets.add(clientSocket);
                }

                String clientIP = clientSocket.getInetAddress().getHostAddress();
                int clientPort = clientSocket.getPort();

                // create a new thread with the client's information to send questions to client
                gs.executorService.submit(() -> gs.clientThread(clientSocket));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    // monitor buzzer from all clients (UDP)
    public void buzzerHandler() {
        while (!shutdownFlag) {
            // initialize reply packet
            byte[] replyData = new byte[1024];
            DatagramPacket replyPacket = new DatagramPacket(replyData, replyData.length);

            // use UDP socket
            try {
                buzzerSocket.receive(replyPacket);
            } catch (IOException e) {
                System.out.println("Failed to connect socket. Reason " + e.getMessage());
            }

            // add replies to queue in order received
            gs.buzzerQueue.add(replyPacket.getAddress().getHostAddress() + ":" + replyPacket.getPort());

            // pop from queue to pick player to answer - TO DO
            System.out.println(gs.buzzerQueue.poll()); // printing rn change to logic to let player answer question
        }
    }

    // main
    public static void main(String args[]) { // hi zak (and not fahim) - jjguy
        // initialize game server object
        GameServer.gs = new GameServer();

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
        gs.questions = new String[size];
        // String[] options = new String[4];

        List<String> options = new ArrayList<>();

        int index = 0;
        File questionFile = new File("questions.txt");
        try (Scanner fileScan = new Scanner(questionFile)) {
            while (fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" \\| ");

                gs.questions[index] = parts[0];
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

        for (int i = 0; i < gs.questions.length; i++) {
            System.out.println("Question " + (i + 1) + ": " + gs.questions[i]);
            for (int j = 0; j < 4; j++) {
                System.out.println("Option " + (j + 1) + ": " + options.get(j + (i * 4)));
            }
            System.out.println();
        }

        // initialize thread pool
        gs.executorService = Executors.newFixedThreadPool(5);

        // run threads
        gs.executorService.submit(() -> gs.buzzerHandler());

        gs.executorService.submit(() -> gs.listenThread());

        // gameplay loop
        while (gs.questionNumber <= 20) {
            // game logic

            // if question has progressed, increment
            // base on timer
            // gs.questionNumber++;
        }

        // if questions complete
        gs.shutdownFlag = true;
    }
}
