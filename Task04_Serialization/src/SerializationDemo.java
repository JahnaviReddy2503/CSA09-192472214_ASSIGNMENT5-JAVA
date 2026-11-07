import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class SerializationDemo {
    private SerializationDemo() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Resident original = new Resident("Arun", "192311001", "hash", null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        Resident restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Resident) input.readObject();
        }
        System.out.println("serializedBytes=" + bytes.size());
        System.out.println("connectionAfterRead=" + restored.connection());
    }
}
