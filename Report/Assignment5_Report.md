# CSA09 Programming in Java - Assignment 5 (CO5)

## Data Persistence Engineering - Files and Databases

**Student:** Jahnavi Reddy  
**Register number:** 192472214  
**Repository:** [CSA09-192472214_ASSIGNMENT5-JAVA](https://github.com/JahnaviReddy2503/CSA09-192472214_ASSIGNMENT5-JAVA)  
**Java:** OpenJDK 17.0.21  
**Database:** MySQL/JDBC execution pending because MySQL is not installed or available on PATH.

## Submission status

This report contains the completed Part A designs, runnable local file
experiments, SQL schemas, JDBC transaction/batch implementations, and actual
non-database evidence. Database results and JDBC measurements are deliberately
marked **BLOCKED** rather than invented.

## Contents

1. [Part A](#part-a)
2. [Part B](#part-b)
3. [Complexity and engineering decision](#engineering-decision)
4. [Validation](#validation)

## Part A

The complete answers are maintained in separate readable documents:

| Task | Document |
|---|---|
| A1 | [A1 file format](../Task01_FileFormat/results/A1_file_format.md) |
| A2 | [A2 buffered I/O](../Task02_BufferedIO/results/A2_buffered_io.md) |
| A3 | [A3 DAO pattern](../Task03_DAODesign/results/A3_dao_pattern.md) |
| A4 | [A4 serialization](../Task04_Serialization/results/A4_serialization.md) |
| A5 | [A5 transactions and injection](../Task05_Transactions/results/A5_transactions_injection.md) |

These documents include structured pseudocode, diagrams, alternatives, and
time/space analysis. Part A contains no Java implementations.

## Part B

### B1 - File store and buffered I/O

Source:

- [PassStore.java](../Task06_FileStoreImplementation/src/PassStore.java)
- [LineCounter.java](../Task06_FileStoreImplementation/src/LineCounter.java)
- [B1Benchmark.java](../Task06_FileStoreImplementation/src/B1Benchmark.java)

Compile and run:

```powershell
javac -d out Task06_FileStoreImplementation\src\*.java
java -cp out PassStore
java -cp out B1Runner
java -cp out B1Benchmark
```

Actual smoke-test output:

```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```

The damaged P1002 line has two fields and is skipped. The search continues and
returns `null` because no valid P1002 record exists.

Actual smoke-test counter output is saved in
[line_counter_actual.txt](../Task06_FileStoreImplementation/output/line_counter_actual.txt).
The benchmark output is saved in
[B1_benchmark_actual.csv](../Task06_FileStoreImplementation/results/B1_benchmark_actual.csv).
The benchmark warmed up each method and records three runs, using the median:

| File size | Unbuffered median (ms) | Buffered median (ms) | Lines |
|---:|---:|---:|---:|
| 1 MiB | 1,396 | 1 | 17,190 |
| 10 MiB | 25,001 | 21 | 171,898 |
| 50 MiB | 102,387 | 30 | 859,489 |

Search-growth medians:

| Records | Median find (ms) |
|---:|---:|
| 1,000 | 1 |
| 10,000 | 5 |
| 100,000 | 31 |

The results show the expected linear scan trend and the large reduction in
physical-read overhead from buffering.

### B2 - Adversarial input

Sources:

- [AdversarialFileTest.java](../Task07_SQLInjection/src/AdversarialFileTest.java)
- [LoginQueries.java](../Task07_SQLInjection/src/LoginQueries.java)
- [schema.sql](../Task07_SQLInjection/sql/schema.sql)

The file validation test was executed locally:

```text
valid=[P1004, R1004, Valid, 7, 3800.0]
damaged=null
empty=null
missing=null
result=malformed records were skipped
```

The unsafe and prepared login methods are implemented, but actual SQL injection
execution is **BLOCKED** until the local `csa09_a5_test` database is available.

### B3 - JDBC batch loading

Sources:

- [PassLoader.java](../Task08_JDBC_Batch_Migration/src/PassLoader.java)
- [create_bus_pass.sql](../Task08_JDBC_Batch_Migration/sql/create_bus_pass.sql)

`PassLoader.loadAll` binds all five values, executes batches every 1,000 rows,
executes the remainder, commits on success, rolls back on SQL/runtime failure,
and restores the original auto-commit setting.

Execution is **BLOCKED**: `mysql --version` is unavailable and no Connector/J
jar is present in the workspace. Therefore no database output or timing is
reported as actual.

### B4 - Migration and metadata

[Migration.java](../Task08_JDBC_Batch_Migration/src/Migration.java) validates
field count, required values, route, and amount; it reports invalid lines and
loads valid records transactionally in batches. `printMetadata` reports product,
version, driver, and tables.

File parsing compiles and is executable without a database. Database migration
and metadata output are **BLOCKED** for the same environment reason as B3.

## Engineering decision

For one local counter, a validated text file is simple and inspectable. For 12
campuses, 100 times the volume, and simultaneous counters, a relational
database is the production choice because it provides indexed lookup,
transactions, constraints, concurrent access control, and prepared statements.
Text files remain useful as import/export or recovery artifacts. Java native
serialization is not recommended as the system of record because it is
Java-specific, difficult to evolve safely, and unsafe for untrusted input.

## Validation

| Requirement | Status | Evidence |
|---|---|---|
| A1-A5 written answers | PASS | Part A documents |
| B1 compile and smoke test | PASS | Actual output files |
| B1 benchmark | PASS | Benchmark CSV and table above |
| B2 malformed-file test | PASS | `AdversarialFileTest` output |
| B2 SQL injection run | BLOCKED | MySQL unavailable |
| B3 JDBC tests | BLOCKED | MySQL and Connector/J unavailable |
| B4 file validation | PASS | `Migration.readValidPasses` |
| B4 database migration/metadata | BLOCKED | MySQL unavailable |
| Git history | PASS | Task-wise local commits |
| GitHub push | PASS | Repository URL above |

## Manual environment steps

Install/start local MySQL, create `csa09_a5_test` using the supplied SQL,
install MySQL Connector/J, set `DB_URL`, `DB_USER`, and `DB_PASS`, then run the
B2-B4 JDBC programs and append their actual console output and timings. Do not
commit the password.
