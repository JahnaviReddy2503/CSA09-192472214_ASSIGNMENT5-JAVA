import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class UnsafeLogin {
    private UnsafeLogin() {
    }

    public static String sql(String id, String password) {
        return "SELECT 1 FROM users WHERE id='" + id + "' AND pwd='" + password + "'";
    }

    public static boolean authenticate(Connection connection, String id, String password)
            throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql(id, password))) {
            return result.next();
        }
    }
}
