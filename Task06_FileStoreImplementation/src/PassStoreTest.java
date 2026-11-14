import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

public final class PassStoreTest {
    private PassStoreTest() {
    }

    public static void main(String[] args) throws IOException {
        Path sampleFile = Path.of("Task06_FileStoreImplementation", "input",
                "sample_passes.csv");
        PassStore sampleStore = new PassStore(sampleFile);
        report("full sample after damaged line", sampleStore.find("P2003") != null,
                "finds a valid full record after an invalid record");
        report("full sample route listing", sampleStore.listByRoute(7).size() == 1,
                "lists valid seven-field records by route");

        Path file = Files.createTempFile("pass-store-test-", ".csv");
        try {
            Files.writeString(file, String.join(System.lineSeparator(),
                    "P1001,R1001,Arun,12,4500.0",
                    "P1002,DAMAGED",
                    "P1003,R1003,Divya,7,3800.0",
                    "P1004,R1004,BadRoute,route,100.0",
                    "P1005,R1005,BadAmount,8,NaN") + System.lineSeparator());
            PassStore store = new PassStore(file);
            report("find existing", Arrays.equals(store.find("P1003"),
                    new String[]{"P1003", "R1003", "Divya", "7", "3800.0"}),
                    "returns the requested valid record");
            report("find missing", store.find("P9999") == null,
                    "returns null when the ID is absent");
            report("skip damaged line", store.find("P1003") != null,
                    "continues after a wrong-field-count line");
            report("skip non-numeric fields", store.find("P1004") == null
                            && store.find("P1005") == null,
                    "rejects a non-integer route and non-finite amount");
            report("list by route", store.listByRoute(7).size() == 1
                            && "P1003".equals(store.listByRoute(7).get(0)[0]),
                    "returns only valid records on the selected route");
            store.add("P2001", "R2001", "Name|WithDelimiter", 5,
                    "Stop\\North\nPlatform", LocalDate.of(2026, 12, 31), 2500.0);
            String[] fullRecord = store.find("P2001");
            report("seven-field encoding", fullRecord != null && fullRecord.length == 7
                            && "Name|WithDelimiter".equals(fullRecord[2])
                            && "Stop\\North\nPlatform".equals(fullRecord[4]),
                    "preserves the full record and escaped delimiters");
            Path missingFile = file.resolveSibling("missing-" + System.nanoTime() + ".csv");
            PassStore missingStore = new PassStore(missingFile);
            report("missing file", missingStore.find("P9999") == null
                            && missingStore.listByRoute(1).isEmpty(),
                    "returns no record and an empty route list");
            missingStore.add("P3001", "R3001", "Created", 1, 10.0);
            report("add creates missing file", "P3001".equals(missingStore.find("P3001")[0]),
                    "creates a new file on first add");
            Files.deleteIfExists(missingFile);
            Path emptyFile = Files.createTempFile("empty-pass-store-", ".csv");
            try {
                PassStore emptyStore = new PassStore(emptyFile);
                report("empty file", emptyStore.find("P9999") == null
                                && emptyStore.listByRoute(1).isEmpty(),
                        "returns no record without throwing");
            } finally {
                Files.deleteIfExists(emptyFile);
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void report(String test, boolean passed, String verifies) {
        System.out.printf("%s | %s | %s%n", test, passed ? "PASS" : "FAIL", verifies);
        if (!passed) {
            throw new AssertionError(test);
        }
    }
}
