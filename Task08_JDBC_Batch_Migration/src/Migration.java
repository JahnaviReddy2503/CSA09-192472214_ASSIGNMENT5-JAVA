import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class Migration {
    private Migration() {
    }

    public static List<String[]> readValidPasses(Path file, List<String> failures)
            throws IOException {
        List<String[]> valid = new ArrayList<>();
        if (!Files.exists(file)) {
            failures.add("FILE_MISSING: " + file);
            return valid;
        }
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    failures.add("line " + lineNumber + ": expected 5 fields");
                    continue;
                }
                try {
                    int route = Integer.parseInt(fields[3]);
                    double amount = Double.parseDouble(fields[4]);
                    if (fields[0].isBlank() || fields[1].isBlank()
                            || fields[2].isBlank() || route <= 0
                            || !Double.isFinite(amount) || amount < 0) {
                        throw new IllegalArgumentException("invalid field value");
                    }
                    valid.add(fields);
                } catch (IllegalArgumentException error) {
                    failures.add("line " + lineNumber + ": " + error.getMessage());
                }
            }
        }
        return valid;
    }

    public static int migrate(Connection connection, List<String[]> passes) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) "
                + "VALUES (?, ?, ?, ?, ?)";
        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int count = 0;
            for (String[] pass : passes) {
                statement.setString(1, pass[0]);
                statement.setString(2, pass[1]);
                statement.setString(3, pass[2]);
                statement.setInt(4, Integer.parseInt(pass[3]));
                statement.setDouble(5, Double.parseDouble(pass[4]));
                statement.addBatch();
                count++;
                if (count % 1000 == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            connection.commit();
            return count;
        } catch (SQLException | RuntimeException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    public static void printMetadata(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        System.out.println("product=" + metadata.getDatabaseProductName());
        System.out.println("version=" + metadata.getDatabaseProductVersion());
        System.out.println("driver=" + metadata.getDriverName());
        System.out.println("driverVersion=" + metadata.getDriverVersion());
        try (ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                System.out.println("table=" + tables.getString("TABLE_NAME"));
            }
        }
    }
}
