import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PassStore {
    private final Path file;

    public PassStore(Path file) {
        this.file = file;
    }

    public void add(String passId, String regNo, String name, int route, double amount)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(String.join(",", passId, regNo, name,
                    String.valueOf(route), String.valueOf(amount)));
            writer.newLine();
        }
    }

    public String[] find(String passId) throws IOException {
        if (!Files.exists(file)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    continue;
                }
                if (fields[0].equals(passId)) {
                    return fields;
                }
            }
        }
        return null;
    }

    public List<String[]> listByRoute(int route) throws IOException {
        List<String[]> matches = new ArrayList<>();
        if (!Files.exists(file)) {
            return matches;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (fields.length == 5 && Integer.toString(route).equals(fields[3])) {
                    matches.add(fields);
                }
            }
        }
        return matches;
    }

    public static void main(String[] args) throws IOException {
        Path temporaryFile = Files.createTempFile("passes", ".csv");
        PassStore store = new PassStore(temporaryFile);
        try {
            store.add("P1001", "192311001", "Arun", 12, 4500);
            Files.writeString(temporaryFile, "P1002,DAMAGED LINE\n",
                    StandardOpenOption.APPEND);
            store.add("P1003", "192311003", "Divya", 7, 3800);
            System.out.println(Arrays.toString(store.find("P1003")));
            System.out.println(Arrays.toString(store.find("P1002")));
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }
}
