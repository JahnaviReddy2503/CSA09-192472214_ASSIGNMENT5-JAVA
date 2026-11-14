import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Migration {
    private static final int BATCH_SIZE = 1000;
    private static final Path DEFAULT_INPUT =
            Path.of("Task08_JDBC_Batch_Migration", "input", "existing_passes.csv");
    private static final Path REPORT =
            Path.of("Task08_JDBC_Batch_Migration", "output", "migration_report.txt");

    private Migration() {
    }

    public static void main(String[] args) throws Exception {
        Path input = args.length == 0 ? DEFAULT_INPUT : Path.of(args[0]);
        ValidationResult validation = readAndValidate(input);
        MigrationReport report = new MigrationReport(validation.linesRead(),
                validation.valid().size(), validation.rejections());
        boolean migrationStarted = false;
        try (Connection connection = DatabaseConnection.getConnection()) {
            migrationStarted = true;
            int inserted = migrate(connection, validation.valid());
            report.complete(inserted, 0);
            writeReport(report);
            System.out.print(report);
        } catch (IOException | SQLException | RuntimeException error) {
            report.fail(error.getMessage(), migrationStarted ? validation.valid().size() : 0,
                    migrationStarted ? 0 : validation.valid().size());
            writeReport(report);
            System.out.print(report);
            throw error;
        }
    }

    static ValidationResult readAndValidate(Path file) throws IOException {
        List<String[]> valid = new ArrayList<>();
        List<String> rejections = new ArrayList<>();
        Set<String> passIds = new HashSet<>();
        int linesRead = 0;
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                linesRead++;
                String reason = PassStore.validationError(line);
                if (reason == null && !passIds.add(line.split(",", -1)[0])) {
                    reason = "duplicate pass ID";
                }
                if (reason == null) {
                    valid.add(line.split(",", -1));
                } else {
                    rejections.add("line " + linesRead + ", raw=[" + line + "], reason=" + reason);
                }
            }
        }
        return new ValidationResult(linesRead, valid, rejections);
    }

    static int migrate(Connection connection, List<String[]> passes) throws SQLException {
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
                if (count % BATCH_SIZE == 0) {
                    statement.executeBatch();
                }
            }
            statement.executeBatch();
            connection.commit();
            return count;
        } catch (SQLException | RuntimeException error) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw error;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private static void writeReport(MigrationReport report) throws IOException {
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, report.toString());
    }

    record ValidationResult(int linesRead, List<String[]> valid, List<String> rejections) {
    }
}
