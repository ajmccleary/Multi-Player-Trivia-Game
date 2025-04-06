package Game;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    //static variables
    private static GameServer gs;
    private static int portNum = 8001;
    private static DatagramSocket buzzerSocket; //UDP buzzer socket
    private static ServerSocket gameSocket; //TCP game socket
    private static String[] questions; //20 questions
    private static List<String> options, answers; //options is four per questions, answers is one per question
    
    //instance variables
    private ExecutorService executorService;
    private ConcurrentLinkedQueue<String> buzzerQueue = new ConcurrentLinkedQueue<String>();
    private HashMap<String, Integer> clients = new HashMap<String, Integer>(); //clientID ("[ip]:[port]") and current score
    private int questionNumber = 0;
    private boolean shutdownFlag, timerEndedFlag, nextQuestionFlag = false;
    
    //constructor method
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

        // initialize array to hold questions
        questions = new String[size];

        //initialize options to hold all options
        options = new ArrayList<>();

        //initialize answers to hold all answers
        answers = new ArrayList<>();

        int index = 0;
        File questionFile = new File("questions.txt");
        try (Scanner fileScan = new Scanner(questionFile)) {
            while (fileScan.hasNextLine()) {
                String[] parts = fileScan.nextLine().split(" \\| ");

                questions[index] = parts[0];
                for (int i = 1; i <= 5; i++) {
                    if (i <= 4)
                        options.add(parts[i]);
                    else
                        answers.add(parts[i]);
                }
                index++;
            }
        } catch (FileNotFoundException e) {
            System.out.println("questions.txt not found in root directory. Exiting program");
            System.exit(1);
        }
    }

    //detect when clients attempt to connect, create new thread per client
    public void listenThread() {
        while (!gs.shutdownFlag) {
            try {
                //receive prcoess and store data from client attempting to connect
                Socket clientSocket = gameSocket.accept();

                //create a new thread with the client's information to send questions to client
                gs.executorService.submit(() -> gs.clientThread(clientSocket));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    //send question thread method (uses TCP)
    public void clientThread (Socket clientSocket) {
        //store clientID as "[ip]:[port]"
        String clientID = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        
        //initialize score to 0 (value) and tie to clientID (key)
        clients.put(clientID, 0);

        System.out.println("DEBUG: Client thread started: " + clientID);

        //periodically blast question and question data to client
        try (OutputStream out = clientSocket.getOutputStream();
            InputStream in = clientSocket.getInputStream()) {
                               
                //Send each question and its options to the client
                // for(int i = 0; i< questions.length; i++){
                    StringBuilder questionData = new StringBuilder(questions[gs.questionNumber]);
                    for(int j = 0; j < 4; j++){
                        questionData.append(" ; ").append(options.get(j + (gs.questionNumber * 4)));
                       
                    }
                    questionData.append("\n");
                    // Sends the questions and options to the client
                    out.write(questionData.toString().getBytes());
                    out.flush();
                    System.out.println("Fuck : " + questionData.toString());
                    
                    //stall until timer ends
                    while (!gs.timerEndedFlag)
                        Thread.sleep(1000);
                // }

                while (!gs.shutdownFlag) {
                //run on one thread at a time
                synchronized (gs.buzzerQueue) {
                    //if question timer ends
                    if (gs.timerEndedFlag) {
                        System.out.println("DEBUG TIMER ENDED CLIENT THREAD");

                        if (gs.buzzerQueue.peek().equals(clientID)) {
                            System.out.println("DEBUG: " + clientID + " was first in queue!");

                            //send ack message to winner
                            out.write("ack".getBytes()); //client knows it can now answer question
                            
                            //remove winner from queue
                            gs.buzzerQueue.poll(); 

                            //initialize holder for winner response
                            byte[] response = new byte[1024];

                            //timer runs ten seconds - DEV replace at some point with real timer
                            Thread.sleep(10000);

                            //get value of current score
                            int currentScore = clients.get(clientID);

                            //if no response received
                            if (in.read(response) == -1) {
                                //decrement currentScore
                                currentScore -= 20;

                                //for DEBUGGING purposes
                                // gs.nextQuestionFlag = false;
                            }

                            //if client answer received is equal to the correct answer stored for the current question
                            else if (new String(response).equals(answers.get(questionNumber))) {
                                //send "correct" message to client
                                out.write("correct".getBytes()); //client now knows its score increased

                                //increment currentScore
                                currentScore += 10;

                                //reset timerEndedFlag to false
                                gs.timerEndedFlag = false;

                                //set next question flag to true
                                gs.nextQuestionFlag = true;
                                
                            } else {
                                //send "wrong" message to client
                                out.write("wrong".getBytes()); //client now knows its score decreased

                                //decrement currentScore
                                currentScore -= 10;
                            }

                            //update current score for a given clientID
                            clients.put(clientID, currentScore);

                        } else { //client not on top of queue
                            //send negative ack message
                            out.write("negative-ack".getBytes()); //client now knows it was not first in queue
                        }
                    }
                }
            }
        } catch (IOException e) {
            //print error
            System.err.println("Communication error with client " + clientID + ": " + e.getMessage());

            //remove client from queue upon disconnect
            gs.buzzerQueue.remove(clientID);

        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    //monitor buzzer from all clients (UDP)
    public void UDPThread() {
        while (!shutdownFlag) {
            System.out.println( "Waiting for buzzer signal..."); //debug message to show waiting state
            //initialize reply packet
            byte[] incomingData = new byte[1024]; //buffer for incoming data
            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            try {
                buzzerSocket.receive(incomingPacket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            //deserialize received packet
            BuzzerProtocol receivedPacket = null;
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(incomingPacket.getData());
                ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                receivedPacket = (BuzzerProtocol) objectInputStream.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            // add replies to queue in order received - DEV replace with protocol which just sends timestamp and port num
            gs.buzzerQueue.add(incomingPacket.getAddress().getHostAddress() + ":" + receivedPacket.getPort()); //use TCP portNum for later usage

            //queue is then handled in seperate thread clientThreads
        }
    }
    
    //main
    public static void main (String args[]) { //hi zak (and not fahim) - jjguy
        //print start message
        System.out.println("Game Server Starting...");

        //initialize game server object
        GameServer.gs = new GameServer();

        //initialize thread pool
        gs.executorService = Executors.newFixedThreadPool(10); //DEV do math for this at some point

        //run threads
        gs.executorService.submit(() -> gs.UDPThread());

        gs.executorService.submit(() -> gs.listenThread());

        // gameplay loop
        while (gs.questionNumber <= 20) {
            //if there is more than one client, begin game
            if (gs.clients.size() > 0) {
                try {
                    System.out.println("DEBUG: Starting question " + (gs.questionNumber + 1) + " out of 20");

                    //timer for 20 seconds
                    Thread.sleep(20000);

                    //when timer ends, set flag to true
                    gs.timerEndedFlag = true;

                    //increment question number
                    gs.questionNumber++;

                    //pause until next question signal sent
                    while (!gs.nextQuestionFlag) {
                        Thread.sleep(1000);
                    }

                    //reset next question flag to false
                    gs.nextQuestionFlag = false;

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        // if questions complete
        gs.shutdownFlag = true;

        
        //winner logic here?
    }
}
