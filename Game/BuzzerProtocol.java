package Game;

import java.io.Serializable;
import java.time.Instant;

public class BuzzerProtocol implements Serializable {
    private int TCP_PORT;
    private Instant timeSent;
    private int questionNumber;

    public BuzzerProtocol (int port, int questionNumber) {
        this.TCP_PORT = port;
        this.timeSent = Instant.now();
        this.questionNumber = questionNumber;
    }

    public int getPort() {
        return TCP_PORT;
    }


    public int getQuestionNumber(){
        return questionNumber;
    }
}
