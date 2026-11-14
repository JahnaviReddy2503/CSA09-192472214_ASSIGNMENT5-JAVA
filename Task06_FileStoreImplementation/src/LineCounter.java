import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public final class LineCounter {
    public record Result(long lines, long readCalls) {
    }

    private LineCounter() {
    }

    public static long countUnbuffered(Path path) throws IOException {
        return countUnbufferedWithMetrics(path).lines();
    }

    public static Result countUnbufferedWithMetrics(Path path) throws IOException {
        long lines = 0;
        long calls = 0;
        try (FileInputStream input = new FileInputStream(path.toFile())) {
            int value;
            while ((value = input.read()) != -1) {
                calls++;
                if (value == '\n') {
                    lines++;
                }
            }
            calls++;
        }
        return new Result(lines, calls);
    }

    public static long countBuffered(Path path) throws IOException {
        return countBufferedWithMetrics(path).lines();
    }

    public static Result countBufferedWithMetrics(Path path) throws IOException {
        long lines = 0;
        long calls = 0;
        byte[] buffer = new byte[8192];
        try (BufferedInputStream input = new BufferedInputStream(
                new FileInputStream(path.toFile()), buffer.length)) {
            int used;
            while ((used = input.read(buffer)) != -1) {
                calls++;
                for (int i = 0; i < used; i++) {
                    if (buffer[i] == '\n') {
                        lines++;
                    }
                }
            }
            calls++;
        }
        return new Result(lines, calls);
    }
}
