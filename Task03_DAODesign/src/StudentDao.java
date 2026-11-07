import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StudentDao implements Dao<Student, Integer> {
    private final Connection connection;

    public StudentDao(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Student insert(Student student) {
        String sql = "INSERT INTO student(student_id, name, email, department) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, student);
            statement.executeUpdate();
            return student;
        } catch (SQLException error) {
            throw new IllegalStateException("Could not insert student", error);
        }
    }

    @Override
    public Optional<Student> findById(Integer id) {
        String sql = "SELECT student_id, name, email, department FROM student WHERE student_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Could not find student", error);
        }
    }

    @Override
    public List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_id, name, email, department FROM student";
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                students.add(map(result));
            }
            return students;
        } catch (SQLException error) {
            throw new IllegalStateException("Could not list students", error);
        }
    }

    @Override
    public boolean update(Student student) {
        String sql = "UPDATE student SET name = ?, email = ?, department = ? WHERE student_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.name());
            statement.setString(2, student.email());
            statement.setString(3, student.department());
            statement.setInt(4, student.studentId());
            return statement.executeUpdate() == 1;
        } catch (SQLException error) {
            throw new IllegalStateException("Could not update student", error);
        }
    }

    @Override
    public boolean delete(Integer id) {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM student WHERE student_id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException error) {
            throw new IllegalStateException("Could not delete student", error);
        }
    }

    private static void bind(PreparedStatement statement, Student student) throws SQLException {
        statement.setInt(1, student.studentId());
        statement.setString(2, student.name());
        statement.setString(3, student.email());
        statement.setString(4, student.department());
    }

    private static Student map(ResultSet result) throws SQLException {
        return new Student(result.getInt("student_id"), result.getString("name"),
                result.getString("email"), result.getString("department"));
    }
}
