# Final Verification Report

**Assignment:** Data Persistence Engineering — Files and Databases (CO5)
**Project:** `CSA09-192472214-Assignment5`

## Required task audit

| Task | Required implementation | Code status | Execution status | Evidence status |
|---|---|---|---|---|
| A1 | File formats, operations, damaged-data handling, fixed-record alternative | Partially completed | Completed and verified | Partially completed |
| A2 | Buffered/unbuffered design, comparison, flowchart, complexity | Completed and verified | Completed and verified | Completed and verified |
| A3 | Generic DAO, Student mapping, architecture and comparison | Completed and verified | Completed and verified | Completed and verified |
| A4 | Serialization, transient fields, compatibility and comparison | Completed and verified | Completed and verified | Completed and verified |
| A5 | Transaction and SQL-injection designs | Partially completed | Completed and verified | Partially completed |
| B1 | PassStore, validation tests, line counter and performance experiments | Completed and verified | Completed and verified | Completed and verified |
| B2 | Unsafe/secure local database tests and damaged-file tests | Implemented but not executed | Blocked by dependency | Partially completed |
| B3 | MySQL statement, prepared, batch, transaction and metadata experiments | Implemented but not executed | Blocked by dependency | Partially completed |
| B4 | MySQL migration, report, metadata and production decision | Implemented but not executed | Blocked by dependency | Partially completed |

### A1-A5 detail

- **A1:** Text and `DataOutputStream` layouts, delimiter escaping, file-operation
  pseudocode, recovery, `RandomAccessFile`, fixed record padding, and complexity
  are documented in `Task01_FileFormat\results\A1_file_format.md`. The official
  SIMATS pseudocode standard was not in the supplied workspace; the document
  flags that its numbered INPUT/OUTPUT/BEGIN/END format needs faculty
  confirmation.
- **A2:** Unbuffered, buffered, byte-array and `Files.readAllLines()` trade-offs,
  flowchart and complexity are documented in
  `Task02_BufferedIO\results\A2_buffered_io.md`.
- **A3:** Generic DAO methods, Student/column mapping, primary key, architecture
  and direct-SQL comparison are documented in
  `Task03_DAODesign\results\A3_dao_pattern.md`.
- **A4:** `SerializationDemo` ran successfully. The output shows that a
  transient connection is restored as `null` and a shared Room reference stays
  shared. The serialized output is
  `Task04_Serialization\results\serialization_actual.txt`.
- **A5:** Transaction, rollback, unsafe SQL and prepared binding designs are in
  `Task05_Transactions\results\A5_transactions_injection.md`. Its pseudocode
  standard remains subject to the same faculty confirmation as A1.

### B1 execution evidence

- All 24 Java source files compiled with `javac --release 17 -Xlint:all` and
  MySQL Connector/J 9.4.0; the compiler reported no warnings.
- All 11 `PassStoreTest` checks passed, including full seven-field records,
  escaping, damaged-row continuation, invalid numeric data, missing/empty
  files, and first-write creation. Results are in
  `Task06_FileStoreImplementation\output\test_results.txt`.
- The required sample output matched exactly:
  `[P1003, 192311003, Divya, 7, 3800.0]` and `null`.
- The damaged-file tests accepted valid rows surrounding invalid rows and
  reported wrong field count, non-numeric amount and empty line:
  `Task07_SQLInjection\results\file_adversarial_actual.txt`.
- Actual 1/10/50 MiB and 1,000/10,000/100,000 record measurements, including
  three trials and averages, are in
  `Task06_FileStoreImplementation\results\B1_benchmark_actual.csv`. The
  measured summary and limitations are in
  `Task06_FileStoreImplementation\results\B1_interpretation.md`.
- These are machine-specific observations. The line-counter benchmark counts
  Java `read` method calls; it does not trace operating-system syscalls.

### Database-dependent work and blockers

- No local MySQL server/service is installed or reachable at
  `127.0.0.1:3306`. Maven and GitHub CLI are also unavailable.
- B2 source and schema compile, and the damaged-file test ran. Unsafe-login
  acceptance and prepared-login rejection were **not** observed. The captured
  current blocker is `Task07_SQLInjection\results\injection_blocked_actual.txt`.
- B3's Java implementations and schema compile, but no real MySQL inserts,
  row counts, metadata, transaction outcomes, or timing results were observed.
  The JDBC execution blockers are under
  `Task08_JDBC_Batch_Migration\results\`.
- B4 migration input validation ran: 7 rows read, 2 valid, 5 rejected, and 0
  inserted. The report lists each rejected row and reason. The database
  connection was not established, so 2 valid records were not attempted;
  migration and metadata database behavior remain unverified.
- `pom.xml` is included but Maven packaging was not run because Maven is absent.
- No screenshots were fabricated; `screenshots\README.md` explains what
  authentic evidence to capture after the local MySQL tests are run.

## Remaining work before submission

1. Install and start local MySQL Community Server. Execute the two schema
   scripts, create the scoped local user, copy
   `config\db.properties.example` to the ignored `config\db.properties`, and
   set a disposable `A5_TEST_PASSWORD` for B2. The exact commands are in
   `README.md`.
2. Run `SqlInjectionRunner`, `JdbcExperiment`, `MultiRowAndLoadData`,
   `DatabaseInfo`, and `Migration`; verify actual database contents and replace
   the blocker-only status with the captured results. Do not invent timings.
3. If faculty requires a particular SIMATS pseudocode template, supply it and
   align A1, A2, A3 and A5 with that standard.
4. Capture screenshots of genuine local execution and place them in the
   documented screenshot folders.
5. Maven validation remains optional for running the provided `javac` build;
   install Maven and run the README commands if Maven packaging evidence is
   required.
6. Confirm the exact-name GitHub repository exists and authenticate before
   changing `origin` and pushing. No push was made to the existing mismatched
   remote.

## Git and credential note

Task-wise commits existed before this work, and the current changes were also
committed in task groups. A prior historical commit contains a synthetic test
credential. The current schema and source no longer hardcode that credential,
and database credentials remain in an ignored local properties file. Git
Git history was not rewritten. Review or replace that historical commit before
publishing if repository policy prohibits retaining it in history.
