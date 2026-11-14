import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PassStoreMap {
    private final Map<String, String[]> passesById = new HashMap<>();

    public PassStoreMap(Path file) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (PassStore.validationError(line) == null) {
                    String[] fields = PassStore.parseFields(line);
                    passesById.putIfAbsent(fields[0], fields);
                }
            }
        }
    }

    public String[] find(String passId) {
        return passesById.get(passId);
    }

    public int size() {
        return passesById.size();
    }
}
