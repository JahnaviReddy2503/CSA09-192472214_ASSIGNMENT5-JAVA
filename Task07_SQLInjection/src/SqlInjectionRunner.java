import java.sql.Connection;
import java.sql.DriverManager;

public final class SqlInjectionRunner {
    private SqlInjectionRunner() {
    }

    public static void main(String[] args) throws Exception {
        String url = required("DB_URL");
        String user = required("DB_USER");
        String password = required("DB_PASS");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String injection = "' OR '1'='1";
            System.out.println("unsafeInjectionAccepted="
                    + LoginQueries.unsafeLogin(connection, injection, injection));
            System.out.println("preparedInjectionAccepted="
                    + LoginQueries.safeLogin(connection, injection, injection));
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        return value;
    }
}
