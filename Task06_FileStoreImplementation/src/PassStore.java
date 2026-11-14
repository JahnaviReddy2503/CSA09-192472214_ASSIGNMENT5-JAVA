import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        if (passId == null || passId.isBlank() || regNo == null || regNo.isBlank()
                || name == null || name.isBlank() || route <= 0
                || !Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Invalid bus-pass field");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(String.join(",", passId, regNo, name,
                    String.valueOf(route), String.valueOf(amount)));
            writer.newLine();
        }
    }

    public void add(String passId, String regNo, String name, int route,
            String boardingStop, LocalDate validityDate, double amount) throws IOException {
        if (passId == null || passId.isBlank() || regNo == null || regNo.isBlank()
                || name == null || name.isBlank() || route <= 0
                || boardingStop == null || boardingStop.isBlank() || validityDate == null
                || !Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Invalid bus-pass field");
        }
        String[] fields = {passId, regNo, name, Integer.toString(route), boardingStop,
                validityDate.toString(), Double.toString(amount)};
        try (BufferedWriter writer = Files.newBufferedWriter(
                file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    writer.write('|');
                }
                writer.write(escape(fields[i]));
            }
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
                if (validationError(line) != null) {
                    continue;
                }
                String[] fields = parseFields(line);
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
                if (validationError(line) == null) {
                    String[] fields = parseFields(line);
                    if (Integer.toString(route).equals(fields[3])) {
                        matches.add(fields);
                    }
                }
            }
        }
        return matches;
    }

    /** Returns null for a valid legacy or full record, otherwise a concise reason. */
    public static String validationError(String line) {
        if (line == null || line.isBlank()) {
            return "empty line";
        }
        String[] fields;
        try {
            fields = parseFields(line);
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
        if (fields.length != 5 && fields.length != 7) {
            return "expected 5 legacy fields or 7 full fields, found " + fields.length;
        }
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isBlank()) {
                return "field " + (i + 1) + " is empty";
            }
        }
        try {
            if (Integer.parseInt(fields[3]) <= 0) {
                return "route must be positive";
            }
        } catch (NumberFormatException error) {
            return "route is not an integer";
        }
        try {
            int amountIndex = fields.length == 7 ? 6 : 4;
            double amount = Double.parseDouble(fields[amountIndex]);
            if (!Double.isFinite(amount) || amount < 0) {
                return "amount must be finite and non-negative";
            }
        } catch (NumberFormatException error) {
            return "amount is not numeric";
        }
        if (fields.length == 7) {
            try {
                LocalDate.parse(fields[5]);
            } catch (DateTimeParseException error) {
                return "validity date must use ISO YYYY-MM-DD format";
            }
        }
        return null;
    }

    static String[] parseFields(String line) {
        if (!line.contains("|")) {
            return line.split(",", -1);
        }
        java.util.List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (escaped) {
                switch (current) {
                    case '\\' -> field.append('\\');
                    case '|' -> field.append('|');
                    case 'n' -> field.append('\n');
                    case 'r' -> field.append('\r');
                    default -> throw new IllegalArgumentException("invalid escape sequence");
                }
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '|') {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (escaped) {
            throw new IllegalArgumentException("unfinished escape sequence");
        }
        fields.add(field.toString());
        return fields.toArray(String[]::new);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|")
                .replace("\n", "\\n").replace("\r", "\\r");
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
