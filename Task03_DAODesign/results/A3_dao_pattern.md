# Task A3 - Data-Access Object Pattern (5 marks)

## Aim

Centralize persistence logic so StudentDB, CourseDB, and MarksDB do not repeat
connection, statement, mapping, error, and resource-management code.

## A3.1 Common and differing steps

Common steps are opening a connection, preparing SQL, binding values, executing
the statement, mapping a result row, handling `SQLException`, and closing JDBC
resources. The differing parts are the table name, primary-key type, columns,
SQL statements, object mapping, and validation rules.

```text
Service -> StudentDao / CourseDao / MarksDao -> JDBC -> database table
             shared DAO contract, entity-specific SQL and mapping
```

## A3.2 Generic DAO contract

The compilable example is in [`Dao.java`](../src/Dao.java),
[`Student.java`](../src/Student.java), and
[`StudentDao.java`](../src/StudentDao.java). `StudentDao` uses
`PreparedStatement`, try-with-resources, and a row-mapping helper.

**INTERFACE Dao<T, K>**

```text
OPERATION insert(entity: T) RETURNS T
OPERATION findById(id: K) RETURNS Optional<T>
OPERATION findAll() RETURNS List<T>
OPERATION update(entity: T) RETURNS boolean
OPERATION delete(id: K) RETURNS boolean
```

**ALGORITHM StudentDao.findById**

**INPUT:** student ID `id`  
**OUTPUT:** a `Student`, or empty result

1. BEGIN.
2. Prepare `SELECT student_id, name, email, department FROM student WHERE student_id = ?`.
3. Bind `id`.
4. Execute the query.
5. IF a row exists THEN map columns to a Student and return it.
6. ELSE return an empty result.
7. END.

`StudentDao.insert`, `update`, and `delete` follow the same resource and
exception pattern, with entity-specific SQL. A service uses the interface and
does not know whether the implementation uses JDBC or another store.

## A3.3 Student-to-table mapping

| Student property | SQL column | Role/type |
|---|---|---|
| `studentId` | `student_id` | `PRIMARY KEY`, integer/string as specified |
| `name` | `name` | `VARCHAR`, required |
| `email` | `email` | `VARCHAR`, unique if required |
| `department` | `department` | `VARCHAR` |

The primary key is bound in `WHERE` clauses for lookup, update, and delete.
Generated keys are read after insert only if the schema generates them.

## Alternative Concept and Complexity Analysis

When SQL is placed directly in every service or GUI class, a column rename
requires changing every duplicated query and mapper. With a DAO, the schema
change is normally localized to the relevant DAO and tests. Both designs have
`O(1)` database round trips for one keyed lookup and `O(n)` work for returning
`n` rows; DAO abstraction adds negligible in-memory overhead. `findAll` uses
`O(n)` result memory unless rows are streamed.

| Design | Maintainability | Key lookup | Full list space |
|---|---|---:|---:|
| Generic DAO | mapping centralized | `O(1)` indexed DB lookup | `O(n)` |
| SQL in GUI/service | duplicated and fragile | same DB complexity | `O(n)` |

## Conclusion

DAO separates business logic from persistence details and makes schema changes,
testing, and replacement of the storage implementation safer without changing
the asymptotic database cost.
