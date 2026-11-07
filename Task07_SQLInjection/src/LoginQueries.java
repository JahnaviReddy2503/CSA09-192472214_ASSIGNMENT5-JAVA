import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class LoginQueries {
    private LoginQueries() {
    }

    public static boolean unsafeLogin(Connection connection, String id, String password)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id='" + id + "' AND pwd='" + password + "'";
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            return result.next();
        }
    }

    public static boolean safeLogin(Connection connection, String id, String password)
            throws SQLException {
        String sql = "SELECT 1 FROM users WHERE id = ? AND pwd = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
