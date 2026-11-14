# B4 - Migration and Production Decision (5 marks)

## B4.1 - Text Migration (2 marks)

`Migration` reads `input/existing_passes.csv`, validates the five fields using
the shared `PassStore` validation rules, rejects duplicate IDs, and retains the
line number, raw line, and reason for each rejected record. Valid rows are
inserted using `PreparedStatement` batches in one transaction. A database
failure rolls back and the report distinguishes failed rows from rows that
were not attempted.

```text
CSV -> validate each line -> invalid lines -> report(line, raw value, reason)
                         -> valid rows -> PreparedStatement batch transaction
                                             | success: commit
                                             | failure: rollback
```

The file was executed through validation on this machine. The actual report
records seven lines, two valid rows, and five rejected rows. MySQL was not
available, so zero rows were inserted and the two valid rows were marked not
attempted because the local connection configuration is missing. See
`output/migration_report.txt` and `output/migration_execution.txt`.

## B4.2 - Database Metadata (1 mark)

`DatabaseInfo` calls `DatabaseMetaData` to display the product, version, JDBC
driver, driver version, and available tables. The same printer also reports
column names, SQL types, column count, and precision. This is useful during a
server move because an engineer can check server/driver compatibility and
confirm expected tables and column shapes before starting a migration.

**Execution status:** Not executed against MySQL because no local database
server is available. The program and its JDBC path compile.

```text
Connection -> DatabaseMetaData -> product/version, JDBC driver/version
                              -> available table names
```

## B4.3 - Production Decision (2 marks)

For 12 campuses, a shared relational database is the production choice. It
provides indexed pass-ID lookups, constraints, transactions, controlled user
privileges, and concurrent access coordination. Text files are easy to inspect
but scans grow linearly and concurrent writes require application-level
locking. Java serialization is Java-specific, difficult to query, and unsafe
for untrusted input.

The B1 measurements show that a file scan becomes slower as the record count
grows, while buffering improves streaming I/O. B3 MySQL timings are unavailable
in this environment; therefore the scale decision is based on the database
features required by the scenario, not invented timing values. When actual
MySQL measurements are run, replace this evidence statement with the observed
table from B3.

| Requirement | Text file | Java serialization | Relational database |
|---|---|---|---|
| Scalability | Linear scans; suitable for modest local data | Whole-object graphs can consume large memory | Indexes and query planning support larger collections |
| Concurrent counters | Requires file locks and atomic updates | Requires locks around shared files | Transactions and row/table locks |
| Searching | Sequential scan unless a separate index is maintained | Usually deserialize/scan the graph | Indexed lookup and SQL predicates |
| Data integrity | Application-level validation and recovery | Class-level compatibility rules | Constraints, transactions, and typed columns |
| Security | File permissions and validation | Do not deserialize untrusted data | Least-privilege users and prepared statements |
| Performance | Good for simple sequential local access | Convenient for trusted Java object graphs | Efficient indexed and concurrent operations; benchmark on target hardware |
| Maintainability | Format changes need explicit parsers/migrations | Java-specific class evolution | Schema migrations and metadata checks |

### Alternative Concept and Complexity Analysis

One serialized file per campus avoids simultaneous writes to a single file but
requires reading up to 12 files to search all campuses: `O(total n)` without
an index and about 12 coordinated updates for a cross-campus operation. Its
space is `O(total n)` plus repeated class metadata. Concurrent counters need
file locks and careful atomic replacement; lock failures and stale copies can
cause lost updates. A shared database indexes pass IDs and uses transactions,
locks, and constraints to coordinate counters. The database is the simpler
correct design for this requirement.

**Evidence Expected:** Rejection and migration status
(`output/migration_report.txt`); metadata source
(`src/DatabaseInfo.java`, `src/DatabaseMetaDataPrinter.java`); B1 measurements
(`../Task06_FileStoreImplementation/results/B1_benchmark_actual.csv`); database
measurement blocker and final decision (B3_RESULTS.md and this section).
