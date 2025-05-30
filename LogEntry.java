import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {
    private final String resource;
    private final String fromPeer;
    private final String toPeer;
    private final LocalDateTime timestamp;
    private final boolean success;

    public LogEntry(String resource, String fromPeer, String toPeer, boolean success) {
        this.resource = resource;
        this.fromPeer = fromPeer;
        this.toPeer = toPeer;
        this.timestamp = LocalDateTime.now();
        this.success = success;
    }

    @Override
    public String toString() {
        return String.format("- %s %s da: %s a: %s %s",
            timestamp.format(DateTimeFormatter.ofPattern("HH:mm")),
            resource,
            fromPeer,
            toPeer,
            success ? "(successo)" : "(fallito)");
    }
}