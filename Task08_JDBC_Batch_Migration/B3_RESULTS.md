# B3 - JDBC Insert Strategies (8 marks)

## B3.1 - Correctness and Metadata (2 marks)

`JdbcExperiment` inserts and verifies 10 rows with `Statement`,
`PreparedStatement`, parameterized batch, and `PassLoader` batch. It then uses
`ResultSetMetaData` to print column count, names, SQL types, and precision.
The SQL schema is in `sql/create_bus_pass.sql`.

**Execution status:** Blocked. The local MySQL service is absent and the
assignment database credentials have not been configured. No JDBC row counts
or metadata have been represented as observed results.

```text
same generated rows -> Statement / PreparedStatement / batches -> MySQL
                                               |                    |
                                               +--------------------+
                                             SELECT count + metadata
```

## B3.2 - Repeated Insertion Benchmark (2 marks)

The harness uses the same deterministic rows for 1,000, 10,000, and 100,000
passes, clears the table between cases, warms each JDBC strategy once with 100
rows, records three timings in milliseconds, and
verifies each row count. The benchmark includes Statement, PreparedStatement,
PreparedStatement batching, and the completed `PassLoader.loadAll`.

`CallableStatement` is intended for stored procedures; this assignment defines
no stored procedure, so adding one would not improve the required insert
comparison.

| Strategy | Data sizes | Runs | Actual times |
|---|---|---:|---|
| Statement | 1k / 10k / 100k | 3 each | Not measured: MySQL unavailable |
| PreparedStatement | 1k / 10k / 100k | 3 each | Not measured: MySQL unavailable |
| PreparedStatement batch | 1k / 10k / 100k | 3 each | Not measured: MySQL unavailable |
| PassLoader batch | 1k / 10k / 100k | 3 each | Not measured: MySQL unavailable |

The expected engineering trade-off is that an individual counter insert uses
one `PreparedStatement` execution, while a nightly import should use bounded
batches inside one transaction. This is a design recommendation, not a
measurement from this machine.

## B3.3 - Auto-Commit and Rollback (2 marks)

The harness intentionally encounters a non-numeric amount after valid inserts.
It checks the table after an auto-commit run and after a `PassLoader` batch
transaction. The transactional batch includes one already executed 1,000-row
batch before the injected error, so the rollback test covers database work
already sent to MySQL.

**Actual table contents:** Not observed because a MySQL connection could not be
opened. Do not treat source-level expectations as execution evidence.

| Failure mode | Expected effect by design | Actual result |
|---|---|---|
| Auto-commit batch fails after a completed batch of 10 | Earlier committed rows remain | Not observed: MySQL unavailable |
| PassLoader transaction fails after a 1,000-row batch executed | All transaction rows are rolled back | Not observed: MySQL unavailable |

## B3.4 - Multi-Row and LOAD DATA Alternative (2 marks)

`MultiRowAndLoadData` compares parameterized 1,000-row multi-row INSERT
statements and `LOAD DATA LOCAL INFILE`. It reports an explicit
`UNAVAILABLE` result if the local MySQL server or Connector/J disallows local
loading. The configured URL enables the client option, but the server may
still disable `local_infile`.

| Alternative | Time complexity | Working memory | Availability |
|---|---|---|---|
| Parameterized multi-row INSERT | `O(n)` row encoding and database inserts | `O(b)` values for chunk size `b` | MySQL connection required |
| LOAD DATA LOCAL INFILE | `O(n)` file transfer and database load; fewer statement round trips | `O(n)` input file on disk, bounded JDBC memory | Requires client and server permission |

No alternatives were benchmarked because MySQL is unavailable. Actual
measurements, when run, are emitted by `JdbcExperiment` and
`MultiRowAndLoadData`; they are not estimated here.

```text
generated rows -> repeated single-row insert
              -> parameterized batch of b rows
              -> one multi-row statement of b rows
              -> LOAD DATA LOCAL INFILE -> verify row count
```

## Alternative Concept and Complexity Analysis

Single-row Statement and PreparedStatement each issue `n` database
executions. Batch and multi-row strategies reduce round trips to approximately
`ceil(n / b)` for batch size `b`, while retaining `O(n)` database work. The
batch's application-side memory is `O(b)`; the current local benchmark
pre-generates data and therefore also holds `O(n)` fixture memory. A single
counter pass should use PreparedStatement for safe, clear one-row insertion.
For 100,000 nightly records, use the measured faster bounded batch strategy
with one transaction; actual numbers must be collected on the target MySQL
machine before making a performance claim.

**Evidence Expected:** SQL schema and loader
(`sql/create_bus_pass.sql`, `src/PassLoader.java`); executable comparisons
(`src/JdbcExperiment.java`, `src/MultiRowAndLoadData.java`); local blocker
(`results/jdbc_execution_blocked_actual.txt`); interpretation and complexity
(this section).
