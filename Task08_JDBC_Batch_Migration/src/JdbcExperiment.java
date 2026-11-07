import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class JdbcExperiment {
    private JdbcExperiment() {
    }

    public static void main(String[] args) throws Exception {
        String url = required("DB_URL");
        String user = required("DB_USER");
        String password = required("DB_PASS");
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            createTable(connection);
            for (int count : new int[]{10, 1_000, 10_000, 100_000}) {
                System.out.println(count + "," + timed(connection, count, 0));
                System.out.println(count + "," + timed(connection, count, 1));
                System.out.println(count + "," + timed(connection, count, 2));
            }
            printMetadata(connection);
            verifyRollback(connection);
        }
    }

    private static long timed(Connection connection, int count, int mode) throws SQLException {
        clear(connection);
        long start = System.nanoTime();
        if (mode == 0) {
            try (Statement statement = connection.createStatement()) {
                for (String[] pass : passes(count)) {
                    statement.executeUpdate("INSERT INTO bus_pass VALUES ('" + pass[0] + "','"
                            + pass[1] + "','" + pass[2] + "'," + pass[3] + "," + pass[4] + ")");
                }
            }
        } else {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(mode == 1);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bus_pass VALUES (?, ?, ?, ?, ?)")) {
                int row = 0;
                for (String[] pass : passes(count)) {
                    bind(statement, pass);
                    if (mode == 2) {
                        statement.addBatch();
                        if (++row % 1000 == 0) {
                            statement.executeBatch();
                        }
                    } else {
                        statement.executeUpdate();
                    }
                }
                if (mode == 2) {
                    statement.executeBatch();
                    connection.commit();
                }
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static void verifyRollback(Connection connection) throws SQLException {
        clear(connection);
        List<String[]> values = passes(20);
        values.set(10, new String[]{"PFAIL", "RFAIL", "Bad", "12", "not-a-number"});
        try {
            PassLoader.loadAll(connection, values);
        } catch (RuntimeException expected) {
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
                result.next();
                System.out.println("rollbackRows=" + result.getInt(1));
            }
        }
    }

    private static void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bus_pass ("
                    + "pass_id VARCHAR(32) PRIMARY KEY, reg_no VARCHAR(32) NOT NULL,"
                    + "name VARCHAR(120) NOT NULL, route_no INT NOT NULL,"
                    + "amount DECIMAL(10,2) NOT NULL)");
        }
    }

    private static void clear(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM bus_pass");
        }
    }

    private static void bind(PreparedStatement statement, String[] pass) throws SQLException {
        statement.setString(1, pass[0]);
        statement.setString(2, pass[1]);
        statement.setString(3, pass[2]);
        statement.setInt(4, Integer.parseInt(pass[3]));
        statement.setDouble(5, Double.parseDouble(pass[4]));
    }

    private static List<String[]> passes(int count) {
        List<String[]> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new String[]{"P" + i, "R" + i, "Student" + i, "12", "4500.0"});
        }
        return result;
    }

    private static void printMetadata(Connection connection) throws SQLException {
        DatabaseMetaDataPrinter.print(connection);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is not set");
        }
        return value;
    }
}
