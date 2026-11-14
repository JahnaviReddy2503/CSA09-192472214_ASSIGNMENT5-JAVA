# CSA09 Programming in Java — Assignment 5 (CO5)

**Assignment:** Data Persistence Engineering — Files and Databases
**Student:** Jahnavi Reddy
**Register number:** 192472214
**Course:** Programming in Java (CSA09), Course Outcome CO5
**Java:** Microsoft OpenJDK 17.0.21
**GitHub username found in the existing remote:** `JahnaviReddy2503`

The details above were read from this checkout's existing Git metadata and
remote. Please correct them if this workspace belongs to a different student.
The requested repository name is
`CSA09-192472214-Assignment5`. The existing remote is
`https://github.com/JahnaviReddy2503/CSA09-192472214_ASSIGNMENT5-JAVA.git`,
which does **not** have the required name. The exact-name repository has not
been created or pushed from this session.
Intended target URL (verify or create before submission):
[CSA09-192472214-Assignment5](https://github.com/JahnaviReddy2503/CSA09-192472214-Assignment5).

## Environment and evidence status

| Item | Observed value |
|---|---|
| OS | Windows 11 Home, build 10.0.26300, 64-bit |
| CPU | 13th Gen Intel Core i7-1360P, 12 cores / 16 logical processors |
| RAM | 15.6 GiB visible |
| Java compiler/runtime | Microsoft OpenJDK 17.0.21 |
| MySQL | No local MySQL client/service; `127.0.0.1:3306` did not accept a connection |
| MySQL version | Not available; no server to query |
| Maven | Not installed |
| GitHub CLI | Not installed |
| Connector/J | MySQL Connector/J 9.4.0 at `lib\mysql-connector-j-9.4.0.jar` |

The codebase compiles with `javac`; a Maven build definition is included but
could not be tested because Maven is not installed. B1 file experiments ran
locally. B2 SQL login tests, B3 MySQL timings, B4 database migration, and
metadata inspection require a local MySQL service and local credentials. No
database output or timing has been invented.

## Project map

| Path | Assignment content |
|---|---|
| `Task01_FileFormat\results\A1_file_format.md` | A1 design and pseudocode |
| `Task02_BufferedIO\results\A2_buffered_io.md` | A2 buffering design |
| `Task03_DAODesign\results\A3_dao_pattern.md` and `src\` | A3 DAO design/example |
| `Task04_Serialization\results\A4_serialization.md` and `src\` | A4 serialization |
| `Task05_Transactions\results\A5_transactions_injection.md` | A5 transaction/security design |
| `PartA_Complexity_Analysis.md` | Cross-task complexity summary |
| `Task06_FileStoreImplementation\src\` | B1 file store and experiments |
| `Task06_FileStoreImplementation\output\` | B1 actual test output |
| `Task06_FileStoreImplementation\results\` | B1 actual benchmark CSV and interpretation |
| `Task07_SQLInjection\src\`, `sql\`, `input\` | B2 local-only security/file tests |
| `Task07_SQLInjection\results\` | B2 actual file tests and SQL-blocker status |
| `Task08_JDBC_Batch_Migration\src\`, `sql\`, `input\` | B3/B4 JDBC, batch, migration |
| `Task08_JDBC_Batch_Migration\B3_RESULTS.md`, `B4_RESULTS.md` | B3/B4 interpretation and blockers |
| `Report\Assignment5_Report.md` | Consolidated report |
| `FINAL_VERIFICATION_REPORT.md` | Requirement and execution audit |
| `screenshots\README.md` | Authentic screenshot instructions |

The item-by-item audit of A1.1 through B4.3, the complete-code tasks, and
alternative concepts is in `FINAL_VERIFICATION_REPORT.md`.

The official SIMATS pseudocode standard was not present in the workspace.
Part A documents state that they use a consistent numbered INPUT/OUTPUT/
BEGIN/END style and need adjustment if the faculty provides a different
standard.

## Requirements

- JDK 17 or later.
- MySQL Server, started locally, with the MySQL Connector/J driver.
- Optional: Maven 3.9+ for the included `pom.xml`.
- Git. GitHub publishing additionally requires an authenticated account and
  the correctly named repository.

## Compile with plain `javac`

Run from the repository root in PowerShell:

```powershell
$sources = Get-ChildItem -Recurse -Filter *.java |
    Where-Object { $_.FullName -notmatch '\\out\\|\\build-out\\|\\target\\' }
New-Item -ItemType Directory -Force build-out | Out-Null
javac --release 17 -Xlint:all `
    -cp "lib\mysql-connector-j-9.4.0.jar" `
    -d build-out $sources.FullName
```

The connector jar is included for a direct, reproducible JDBC classpath. The
non-database tests can be run with:

```powershell
$cp = "build-out;lib\mysql-connector-j-9.4.0.jar"
java -cp $cp PassStore
java -cp $cp PassStoreTest
java -cp $cp B1Runner
java -cp $cp B1Benchmark
java -cp $cp AdversarialFileTest
java -cp $cp SerializationDemo
```

`B1Benchmark` creates exact 1 MiB, 10 MiB, and 50 MiB temporary files, warms
both line counters, records three runs per case and records line-counter
search growth for 1,000, 10,000, and 100,000 rows. It also measures 100
repeated scans against an in-memory `HashMap`; approximate heap changes are
labelled as estimates. It can take several minutes because the unbuffered case
calls `FileInputStream.read()` once per byte. Generated temporary files are
deleted when each measurement finishes.

The successful `PassStore` output is:

```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```

`PassStore` supports the assignment's five-field complete-code example and an
overloaded seven-field format containing boarding stop and ISO validity date.
The full format escapes `\`, `|`, CR, and LF. Search skips damaged records and
continues after them.

## Maven

`pom.xml` sets Java 17, declares Connector/J 9.4.0, and adds the task source
directories. When Maven is installed and dependency downloads are available:

```powershell
mvn -q -DskipTests package
mvn -q exec:java -Dexec.mainClass=PassStore
```

Maven execution has not been verified on this machine because Maven is absent.
The `javac` build is the validated build path.

## Local MySQL setup — required before B2, B3, and B4 database runs

1. Download MySQL Community Server from the official
   [MySQL Installer](https://dev.mysql.com/downloads/installer/). In the
   installer, select MySQL Server, configure a local root password, and start
   the MySQL Windows service. Verify it is running and accepting connections
   on `127.0.0.1:3306` before continuing.
2. As a local administrator, execute the two SQL files from the repository
   root. Both create/use only the assignment database `transport_db`:

   ```powershell
   & $env:ComSpec /c 'mysql -u root -p < Task07_SQLInjection\sql\schema.sql'
   & $env:ComSpec /c 'mysql -u root -p < Task08_JDBC_Batch_Migration\sql\create_bus_pass.sql'
   ```

3. Create a dedicated local user restricted to this database; choose a
   password locally and never commit it:

   ```sql
   CREATE USER 'assignment_user'@'localhost' IDENTIFIED BY '<choose-a-local-password>';
   GRANT ALL PRIVILEGES ON transport_db.* TO 'assignment_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

   The grant is scoped to `transport_db`; `DROP` permission is needed by
   `TRUNCATE TABLE` in the isolated benchmark database.
4. Copy `config\db.properties.example` to `config\db.properties` and set the
   local password. `config\db.properties` is ignored by Git. The connection
   helper refuses non-local hosts and schemas other than `transport_db`.
5. Before running B2, set a disposable local test-account value:

   ```powershell
   $env:A5_TEST_PASSWORD = '<choose-a-disposable-test-value>'
   ```

   `SqlInjectionRunner` seeds the synthetic `alice` account through a
   PreparedStatement. Do not reuse the database user's password or commit a
   captured login statement.
6. To test `LOAD DATA LOCAL INFILE`, check the local server setting:

   ```sql
   SHOW VARIABLES LIKE 'local_infile';
   ```

   If it is disabled, an administrator may run `SET GLOBAL local_infile=1`.
   If permission is unavailable, the program must report LOAD DATA as
   unavailable; it must not substitute estimated timings.

## Database programs

Compile using the command above, configure the local database, then run from
the repository root:

```powershell
$cp = "build-out;lib\mysql-connector-j-9.4.0.jar"
java -cp $cp SqlInjectionRunner
java -cp $cp AlternativeValidation
java -cp $cp JdbcExperiment
java -cp $cp MultiRowAndLoadData
java -cp $cp DatabaseInfo
java -cp $cp Migration
```

`JdbcExperiment` verifies row counts after every strategy, prints metadata,
compares Statement/PreparedStatement/batch/PassLoader at 10, 1,000, 10,000,
and 100,000 records in three trials plus an average, and checks auto-commit
versus transactional failure.
`MultiRowAndLoadData` compares bound multi-row INSERT with local LOAD DATA.
`Migration` reads `Task08_JDBC_Batch_Migration\input\existing_passes.csv`,
reports invalid rows, and writes its report to
`Task08_JDBC_Batch_Migration\output\migration_report.txt`.

`UnsafeLogin` and `SecureLogin` are called by `SqlInjectionRunner`.
`DatabaseConnection`, `PassLoader`, `DatabaseMetaDataPrinter`, and
`MigrationReport` are support classes and are compiled with the task sources;
they are not standalone command-line programs. `Dao`, `Student`, `StudentDao`,
and `Resident` are model/persistence types; `SerializationDemo` is the runnable
serialization example. The Student DAO has no student schema because no
student database task is part of this assignment.

## Existing actual results

| Test | Result |
|---|---|
| All current Java source files compile under JDK 17 | Passed with `javac -Xlint:all` |
| PassStore expected output | Passed; saved under B1 `output\passstore_actual.txt` |
| PassStore positive, missing, damaged, route, and seven-field escaping tests | Passed; `output\test_results.txt` |
| B1 small line counter smoke test | Passed; `output\line_counter_actual.txt` |
| B2 damaged-file validation | Passed; `Task07_SQLInjection\results\file_adversarial_actual.txt` |
| Serialization object-graph demo | Passed; connection restored as `null` |
| MySQL injection, JDBC timing, metadata, transaction, migration | Blocked; local server/configuration absent |

Detailed and machine-specific measurements appear in
`Task06_FileStoreImplementation\results\B1_benchmark_actual.csv`. B3 does not
have actual database timings until the local MySQL prerequisite is fulfilled.

| B1 input | Unbuffered average | Buffered average |
|---:|---:|---:|
| 1 MiB | 1,419.070 ms | 1.178 ms |
| 10 MiB | 15,462.941 ms | 11.066 ms |
| 50 MiB | 84,438.391 ms | 31.202 ms |

| B3 dataset | Statement | PreparedStatement | Prepared batch |
|---:|---|---|---|
| 10 rows | Not measured: MySQL unavailable | Not measured: MySQL unavailable | Not measured: MySQL unavailable |
| 1,000 rows | Not measured: MySQL unavailable | Not measured: MySQL unavailable | Not measured: MySQL unavailable |
| 10,000 rows | Not measured: MySQL unavailable | Not measured: MySQL unavailable | Not measured: MySQL unavailable |
| 100,000 rows | Not measured: MySQL unavailable | Not measured: MySQL unavailable | Not measured: MySQL unavailable |

## GitHub and commits

The existing local repository has task-wise commits and a configured `origin`,
but that remote's name does not match the required repository name. This
checkout does not have the GitHub CLI, and no push to the mismatched remote was
made. Before submission, create or confirm
`CSA09-192472214-Assignment5`, configure the matching `origin`, authenticate,
and push the existing progressive task commits:

```powershell
git remote set-url origin https://github.com/JahnaviReddy2503/CSA09-192472214-Assignment5.git
git push -u origin HEAD
```

Only run these commands after confirming the exact-name repository exists and
your GitHub authentication is configured. Do not copy `config\db.properties`.

## Screenshots

No screenshots were fabricated. Add your own screenshots after running the
MySQL tests, in the relevant task `results\screenshots\` folder, and update
the evidence references before submission. The required SQL evidence cannot be
captured until MySQL is installed and started.
