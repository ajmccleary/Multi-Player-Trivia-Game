package Game;

import java.io.Serializable;
import java.time.Instant;

public class BuzzerProtocol implements Serializable {
    private int TCP_PORT;
    private Instant timeSent;
    private int questionNumber;
    private String version;

    public BuzzerProtocol (int port, int questionNumber) {
        this.version = ":)";
        this.TCP_PORT = port;
        this.timeSent = Instant.now(); //use to order UDP packets sent
        this.questionNumber = questionNumber;
    }

    public int getPort() {
        return this.TCP_PORT;
    }

    public Instant getTimeSent() {
        return this.timeSent;
    }

    public int getQuestionNumber(){
        return questionNumber;
    }
}
