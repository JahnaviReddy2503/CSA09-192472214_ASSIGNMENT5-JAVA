import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AdversarialFileTest {
    private static final Path DEFAULT_INPUT =
            Path.of("Task07_SQLInjection", "input", "damaged_passes.csv");

    private AdversarialFileTest() {
    }

    public static void main(String[] args) throws IOException {
        Path file = args.length == 0 ? DEFAULT_INPUT : Path.of(args[0]);
        int lineNumber = 0;
        for (String line : Files.readAllLines(file)) {
            lineNumber++;
            String reason = PassStore.validationError(line);
            System.out.printf("line=%d | %s | %s%n", lineNumber,
                    reason == null ? "ACCEPTED" : "REJECTED",
                    reason == null ? line : reason);
        }
        PassStore store = new PassStore(file);
        System.out.println("validSearch="
                + java.util.Arrays.toString(store.find("P1004")));
    }
}
