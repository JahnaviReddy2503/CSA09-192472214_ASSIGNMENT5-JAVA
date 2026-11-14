import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SecureLogin {
    private static final String SQL = "SELECT 1 FROM users WHERE id = ? AND pwd = ?";

    private SecureLogin() {
    }

    public static String sql() {
        return SQL;
    }

    public static boolean authenticate(Connection connection, String id, String password)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SQL)) {
            statement.setString(1, id);
            statement.setString(2, password);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }
}
