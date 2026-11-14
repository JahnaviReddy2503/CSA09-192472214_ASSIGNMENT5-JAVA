import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class SerializationDemo {
    private SerializationDemo() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Resident original = new Resident("Arun", "192311001", "hash", null);
        Room room = new Room("A-01", List.of(original));
        Hostel hostel = new Hostel(List.of(room, room));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(hostel);
        }
        Hostel restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (Hostel) input.readObject();
        }
        Resident restoredResident = restored.rooms().get(0).residents().get(0);
        System.out.println("serializedBytes=" + bytes.size());
        System.out.println("connectionAfterRead=" + restoredResident.connection());
        System.out.println("sharedRoomReferencePreserved="
                + (restored.rooms().get(0) == restored.rooms().get(1)));
    }

    private static final class Hostel implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<Room> rooms;

        private Hostel(List<Room> rooms) {
            this.rooms = new ArrayList<>(rooms);
        }

        private List<Room> rooms() {
            return rooms;
        }
    }

    private static final class Room implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String roomNumber;
        private final List<Resident> residents;

        private Room(String roomNumber, List<Resident> residents) {
            this.roomNumber = roomNumber;
            this.residents = new ArrayList<>(residents);
        }

        private List<Resident> residents() {
            return residents;
        }
    }
}
