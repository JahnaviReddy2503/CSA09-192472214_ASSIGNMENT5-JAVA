import java.sql.Connection;

public final class DatabaseInfo {
    private DatabaseInfo() {
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            DatabaseMetaDataPrinter.print(connection);
        }
    }
}
