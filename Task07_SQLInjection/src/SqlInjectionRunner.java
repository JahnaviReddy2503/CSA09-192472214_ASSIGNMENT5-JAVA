import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class SqlInjectionRunner {
    private SqlInjectionRunner() {
    }

    public static void main(String[] args) throws Exception {
        String testPassword = required("A5_TEST_PASSWORD");
        try (Connection connection = DatabaseConnection.getConnection()) {
            seedLocalTestUser(connection, testPassword);
            String injectionId = "' OR '1'='1' -- ";
            String invalidPassword = "not-a-password";
            System.out.println("normalValid.unsafeSql="
                    + UnsafeLogin.sql("alice", testPassword));
            System.out.println("normalValid.unsafeAccepted="
                    + UnsafeLogin.authenticate(connection, "alice", testPassword));
            System.out.println("normalValid.preparedSql=" + SecureLogin.sql());
            System.out.println("normalValid.safeAccepted="
                    + SecureLogin.authenticate(connection, "alice", testPassword));
            System.out.println("normalInvalid.unsafeSql="
                    + UnsafeLogin.sql("alice", invalidPassword));
            System.out.println("normalInvalid.preparedSql=" + SecureLogin.sql());
            System.out.println("normalInvalid.unsafeAccepted="
                    + UnsafeLogin.authenticate(connection, "alice", invalidPassword));
            System.out.println("normalInvalid.safeAccepted="
                    + SecureLogin.authenticate(connection, "alice", invalidPassword));
            System.out.println("injection.unsafeSql="
                    + UnsafeLogin.sql(injectionId, invalidPassword));
            System.out.println("injection.unsafeAccepted="
                    + UnsafeLogin.authenticate(connection, injectionId, invalidPassword));
            System.out.println("injection.preparedSql=" + SecureLogin.sql());
            System.out.println("injection.safeAccepted="
                    + SecureLogin.authenticate(connection, injectionId, invalidPassword));
        }
    }

    static void seedLocalTestUser(Connection connection, String password) throws SQLException {
        String sql = "INSERT INTO users(id, pwd) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE pwd = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "alice");
            statement.setString(2, password);
            statement.setString(3, password);
            statement.executeUpdate();
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
