import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseMetaDataPrinter {
    private DatabaseMetaDataPrinter() {
    }

    public static void print(Connection connection) throws SQLException {
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
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM bus_pass LIMIT 1")) {
            ResultSetMetaData columns = result.getMetaData();
            System.out.println("columnCount=" + columns.getColumnCount());
            for (int i = 1; i <= columns.getColumnCount(); i++) {
                System.out.println("column=" + columns.getColumnName(i)
                        + ",type=" + columns.getColumnTypeName(i)
                        + ",size=" + columns.getPrecision(i));
            }
        }
    }
}
