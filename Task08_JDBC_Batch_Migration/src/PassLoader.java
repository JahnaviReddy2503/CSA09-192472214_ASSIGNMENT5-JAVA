import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class PassLoader {
    private PassLoader() {
    }

    public static int loadAll(Connection con, List<String[]> passes) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) "
                + "VALUES (?, ?, ?, ?, ?)";
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int n = 0;
            for (String[] pass : passes) {
                if (pass.length != 5) {
                    throw new IllegalArgumentException("Expected five fields");
                }
                ps.setString(1, pass[0]);
                ps.setString(2, pass[1]);
                ps.setString(3, pass[2]);
                ps.setInt(4, Integer.parseInt(pass[3]));
                ps.setDouble(5, Double.parseDouble(pass[4]));
                ps.addBatch();
                n++;
                if (n % 1000 == 0) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            con.commit();
            return n;
        } catch (SQLException | RuntimeException error) {
            try {
                con.rollback();
            } catch (SQLException rollbackError) {
                error.addSuppressed(rollbackError);
            }
            throw error;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }
}
