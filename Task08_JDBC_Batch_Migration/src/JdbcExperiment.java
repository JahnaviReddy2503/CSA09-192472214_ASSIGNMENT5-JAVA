import java.sql.Connection;
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
        try (Connection connection = DatabaseConnection.getConnection()) {
            for (int mode = 0; mode < 4; mode++) {
                timed(connection, passes(100), mode);
            }
            System.out.println("strategy,rows,run,elapsedMs,verifiedRows");
            for (int count : new int[]{10, 1_000, 10_000, 100_000}) {
                List<String[]> dataset = passes(count);
                for (int mode = 0; mode < 4; mode++) {
                    long totalNanos = 0;
                    for (int run = 1; run <= 3; run++) {
                        long elapsedNanos = timed(connection, dataset, mode);
                        totalNanos += elapsedNanos;
                        int rows = rowCount(connection);
                        if (rows != count) {
                            throw new IllegalStateException("Expected " + count
                                    + " inserted rows, found " + rows);
                        }
                        System.out.printf("%s,%d,%d,%.3f,%d%n",
                                modeName(mode), count, run, elapsedNanos / 1_000_000.0, rows);
                    }
                    System.out.printf("%s,%d,AVERAGE,%.3f,%d%n",
                            modeName(mode), count,
                            totalNanos / 3_000_000.0, rowCount(connection));
                }
            }
            printMetadata(connection);
            verifyFailureModes(connection);
        }
    }

    private static long timed(Connection connection, List<String[]> passes, int mode)
            throws SQLException {
        clear(connection);
        long start = System.nanoTime();
        if (mode == 0) {
            try (Statement statement = connection.createStatement()) {
                for (String[] pass : passes) {
                    statement.executeUpdate("INSERT INTO bus_pass "
                            + "(pass_id, reg_no, name, route_no, amount) VALUES ('"
                            + pass[0] + "','" + pass[1] + "','" + pass[2] + "',"
                            + pass[3] + "," + pass[4] + ")");
                }
            }
        } else if (mode == 3) {
            PassLoader.loadAll(connection, passes);
        } else {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(mode == 1);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO bus_pass VALUES (?, ?, ?, ?, ?)")) {
                int row = 0;
                for (String[] pass : passes) {
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
        return System.nanoTime() - start;
    }

    private static void verifyFailureModes(Connection connection) throws SQLException {
        clear(connection);
        List<String[]> values = passes(20);
        values.set(10, new String[]{"PFAIL", "RFAIL", "Bad", "12", "not-a-number"});
        int autoCommitRows = 0;
        try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO bus_pass VALUES (?, ?, ?, ?, ?)")) {
            connection.setAutoCommit(true);
            for (int i = 0; i < values.size(); i++) {
                String[] value = values.get(i);
                bind(insert, value);
                insert.addBatch();
                if ((i + 1) % 10 == 0) {
                    insert.executeBatch();
                    autoCommitRows += 10;
                }
            }
        } catch (RuntimeException expected) {
            System.out.println("autoCommitFailure=" + expected.getMessage());
        }
        System.out.println("autoCommitRowsAfterFailure=" + rowCount(connection));

        clear(connection);
        try {
            PassLoader.loadAll(connection, batchFailureFixture());
        } catch (RuntimeException expected) {
            System.out.println("batchTransactionFailure=" + expected.getMessage());
        }
        System.out.println("batchTransactionRowsAfterFailure=" + rowCount(connection));
        System.out.println("autoCommitRowsInsertedBeforeFailure=" + autoCommitRows);
    }

    private static List<String[]> batchFailureFixture() {
        List<String[]> values = passes(2_000);
        values.set(1_500, new String[]{"PFAIL", "RFAIL", "Bad", "12", "not-a-number"});
        return values;
    }

    private static void clear(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE bus_pass");
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

    private static int rowCount(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static String modeName(int mode) {
        return switch (mode) {
            case 0 -> "Statement";
            case 1 -> "PreparedStatement";
            case 2 -> "PreparedStatementBatch";
            case 3 -> "PassLoaderBatch";
            default -> throw new IllegalArgumentException("Unknown mode " + mode);
        };
    }
}
