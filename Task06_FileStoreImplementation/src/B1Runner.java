import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class B1Runner {
    private B1Runner() {
    }

    public static void main(String[] args) throws IOException {
        Path file = Files.createTempFile("line-count", ".log");
        try {
            Files.writeString(file, "one\ntwo\nthree\n");
            long unbufferedStart = System.nanoTime();
            long unbuffered = LineCounter.countUnbuffered(file);
            long unbufferedNanos = System.nanoTime() - unbufferedStart;
            long bufferedStart = System.nanoTime();
            long buffered = LineCounter.countBuffered(file);
            long bufferedNanos = System.nanoTime() - bufferedStart;
            System.out.println("unbufferedLines=" + unbuffered
                    + ", timeNanos=" + unbufferedNanos);
            System.out.println("bufferedLines=" + buffered
                    + ", timeNanos=" + bufferedNanos);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
