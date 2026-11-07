import java.io.Serializable;
import java.sql.Connection;

public final class Resident implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String registerNumber;
    private final String passwordHash;
    private transient Connection connection;

    public Resident(String name, String registerNumber, String passwordHash,
            Connection connection) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.passwordHash = passwordHash;
        this.connection = connection;
    }

    public Connection connection() {
        return connection;
    }

    public void reconnect(Connection connection) {
        this.connection = connection;
    }
}
