import java.util.List;
import java.util.Optional;

public interface Dao<T, K> {
    T insert(T entity);

    Optional<T> findById(K id);

    List<T> findAll();

    boolean update(T entity);

    boolean delete(K id);
}
