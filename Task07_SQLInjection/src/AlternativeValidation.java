import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class AlternativeValidation {
    private static final String VALIDATION_PATTERN = "^[A-Za-z0-9_-]+$";
    private static final int ITERATIONS = 1000;

    private AlternativeValidation() {
    }

    public static void main(String[] args) throws Exception {
        String testPassword = required("A5_TEST_PASSWORD");
        String injection = "' OR '1'='1' -- ";
        System.out.println("backslashEscapedInjectionSql="
                + escapedSql(injection, "invalid"));
        System.out.println("regexRejectsInjection=" + !isAllowedCharacters(injection));
        System.out.println("regexRejectsLegitimateName="
                + !isAllowedCharacters("O'Reilly"));
        try (Connection connection = DatabaseConnection.getConnection()) {
            SqlInjectionRunner.seedLocalTestUser(connection, testPassword);
            long escapedTotal = measureEscapedStatement(connection, testPassword);
            long regexTotal = measureRegexStatement(connection, testPassword);
            long preparedTotal = measurePreparedStatement(connection, testPassword);
            System.out.printf("iterations=%d,method,avgMicros%n", ITERATIONS);
            System.out.printf("quote-escape,%.3f%n", escapedTotal / 1000.0 / ITERATIONS);
            System.out.printf("regex-check,%.3f%n", regexTotal / 1000.0 / ITERATIONS);
            System.out.printf("PreparedStatement,%.3f%n",
                    preparedTotal / 1000.0 / ITERATIONS);
        }
    }

    private static long measureEscapedStatement(Connection connection, String password)
            throws SQLException {
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String sql = escapedSql("alice", password);
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(sql)) {
                if (!result.next()) {
                    throw new SQLException("Expected the local test login to succeed");
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static long measureRegexStatement(Connection connection, String testPassword)
            throws SQLException {
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String id = "alice";
            String password = testPassword;
            if (!isAllowedCharacters(id) || !isAllowedCharacters(password)) {
                throw new IllegalArgumentException("Input does not match the test allowlist");
            }
            String sql = "SELECT 1 FROM users WHERE id='" + id + "' AND pwd='"
                    + password + "'";
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(sql)) {
                if (!result.next()) {
                    throw new SQLException("Expected the local test login to succeed");
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static long measurePreparedStatement(Connection connection, String password)
            throws SQLException {
        long start = System.nanoTime();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM users WHERE id = ? AND pwd = ?")) {
            for (int i = 0; i < ITERATIONS; i++) {
                statement.setString(1, "alice");
                statement.setString(2, password);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Expected the local test login to succeed");
                    }
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static boolean isAllowedCharacters(String value) {
        return value != null && value.matches(VALIDATION_PATTERN);
    }

    private static String escapedSql(String id, String password) {
        return "SELECT 1 FROM users WHERE id='" + backslashEscape(id)
                + "' AND pwd='" + backslashEscape(password) + "'";
    }

    private static String backslashEscape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        return value;
    }
}
