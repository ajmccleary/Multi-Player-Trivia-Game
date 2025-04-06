package Game;

import java.io.Serializable;
import java.time.Instant;

public class BuzzerProtocol implements Serializable {
    private int TCP_PORT;
    private Instant timeSent;

    public BuzzerProtocol (int port) {
        this.TCP_PORT = port;
        this.timeSent = Instant.now();
    }

    public int getPort() {
        return TCP_PORT;
    }
}
