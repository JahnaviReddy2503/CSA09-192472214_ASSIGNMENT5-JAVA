import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class MultiRowAndLoadData {
    private static final int ROWS_PER_STATEMENT = 1000;

    private MultiRowAndLoadData() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("method,rows,run,elapsedMs,verifiedRows");
            for (int rows : new int[]{1_000, 10_000, 100_000}) {
                for (int run = 1; run <= 3; run++) {
                    long multiRowMs = measureMultiRow(connection, rows);
                    System.out.printf("multi-row,%d,%d,%d,%d%n",
                            rows, run, multiRowMs, countRows(connection));
                    try {
                        long loadDataMs = measureLoadData(connection, rows);
                        System.out.printf("LOAD DATA LOCAL INFILE,%d,%d,%d,%d%n",
                                rows, run, loadDataMs, countRows(connection));
                    } catch (SQLException error) {
                        System.out.printf("LOAD DATA LOCAL INFILE,%d,%d,UNAVAILABLE,%s%n",
                                rows, run, error.getMessage().replace('\n', ' '));
                        break;
                    }
                }
            }
        }
    }

    private static long measureMultiRow(Connection connection, int count) throws SQLException {
        truncate(connection);
        List<String[]> passes = passes(count);
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        long start = System.nanoTime();
        try {
            for (int offset = 0; offset < count; offset += ROWS_PER_STATEMENT) {
                int rows = Math.min(ROWS_PER_STATEMENT, count - offset);
                String sql = multiRowSql(rows);
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    int parameter = 1;
                    for (int row = offset; row < offset + rows; row++) {
                        String[] pass = passes.get(row);
                        statement.setString(parameter++, pass[0]);
                        statement.setString(parameter++, pass[1]);
                        statement.setString(parameter++, pass[2]);
                        statement.setInt(parameter++, Integer.parseInt(pass[3]));
                        statement.setDouble(parameter++, Double.parseDouble(pass[4]));
                    }
                    statement.executeUpdate();
                }
            }
            connection.commit();
            return (System.nanoTime() - start) / 1_000_000;
        } catch (SQLException | RuntimeException error) {
            rollback(connection, error);
            throw error;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private static long measureLoadData(Connection connection, int count)
            throws SQLException, IOException {
        truncate(connection);
        Path csv = Files.createTempFile("assignment-passes-", ".csv");
        try {
            writeCsv(csv, count);
            String localPath = csv.toAbsolutePath().toString()
                    .replace("\\", "/").replace("'", "''");
            String sql = "LOAD DATA LOCAL INFILE '" + localPath + "' INTO TABLE bus_pass "
                    + "FIELDS TERMINATED BY ',' LINES TERMINATED BY '\\n' "
                    + "(pass_id, reg_no, name, route_no, amount)";
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            long start = System.nanoTime();
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
                connection.commit();
                return (System.nanoTime() - start) / 1_000_000;
            } catch (SQLException error) {
                rollback(connection, error);
                throw error;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private static String multiRowSql(int rows) {
        StringBuilder sql = new StringBuilder(
                "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES ");
        for (int row = 0; row < rows; row++) {
            if (row > 0) {
                sql.append(',');
            }
            sql.append("(?, ?, ?, ?, ?)");
        }
        return sql.toString();
    }

    private static List<String[]> passes(int count) {
        List<String[]> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new String[]{"P" + i, "R" + i, "Student" + i, "12", "4500.0"});
        }
        return result;
    }

    private static void writeCsv(Path path, int count) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (int i = 0; i < count; i++) {
                writer.write("P" + i + ",R" + i + ",Student" + i + ",12,4500.0\n");
            }
        }
    }

    private static void truncate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    private static int countRows(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
                var result = statement.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }
}
