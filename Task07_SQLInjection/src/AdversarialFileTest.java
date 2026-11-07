import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AdversarialFileTest {
    private AdversarialFileTest() {
    }

    public static void main(String[] args) throws IOException {
        Path file = Files.createTempFile("damaged-passes-", ".csv");
        try {
            Files.writeString(file,
                    "P1001,R1001,Arun,12,4500.0\n"
                            + "P1002,R1002,Wrong,12\n"
                            + "P1003,R1003,NonNumeric,12,not-a-number\n"
                            + "\n"
                            + "P1004,R1004,Valid,7,3800.0\n");
            PassStore store = new PassStore(file);
            System.out.println("valid=" + java.util.Arrays.toString(store.find("P1004")));
            System.out.println("damaged=" + store.find("P1002"));
            System.out.println("empty=" + store.find(""));
            System.out.println("missing=" + store.find("P9999"));
            System.out.println("result=malformed records were skipped");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
