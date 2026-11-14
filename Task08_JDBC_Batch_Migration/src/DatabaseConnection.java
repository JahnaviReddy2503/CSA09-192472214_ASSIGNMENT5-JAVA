import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final Path CONFIG = Path.of("config", "db.properties");

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws IOException, SQLException {
        if (!Files.isRegularFile(CONFIG)) {
            throw new IOException("Missing " + CONFIG
                    + "; copy config/db.properties.example and set local credentials");
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG)) {
            properties.load(input);
        }
        String url = required(properties, "url");
        validateLocalAssignmentDatabase(url);
        String user = required(properties, "user");
        String password = properties.getProperty("password");
        if (password == null) {
            throw new IOException("Missing required database property: password");
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static void validateLocalAssignmentDatabase(String url) throws IOException {
        String prefix = "jdbc:mysql://";
        if (!url.startsWith(prefix)) {
            throw new IOException("Only a local MySQL JDBC URL is allowed");
        }
        URI address;
        try {
            address = URI.create("http://" + url.substring(prefix.length()));
        } catch (IllegalArgumentException error) {
            throw new IOException("Invalid MySQL JDBC URL", error);
        }
        String path = address.getPath();
        if (!("localhost".equalsIgnoreCase(address.getHost())
                || "127.0.0.1".equals(address.getHost())
                || "::1".equals(address.getHost()))
                || path == null || !"/transport_db".equals(path)) {
            throw new IOException("Database access is restricted to local transport_db");
        }
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Missing required database property: " + key);
        }
        return value;
    }
}
