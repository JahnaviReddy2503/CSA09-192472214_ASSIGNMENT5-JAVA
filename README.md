# CSA09 Programming in Java - Assignment 5

Student name: **[NOT PROVIDED]**  
Register number: **[NOT PROVIDED]**  
Repository link: **https://github.com/JahnaviReddy2503/CSA09-192472214_ASSIGNMENT5-JAVA**  
Java tested: OpenJDK 17.0.21

This workspace contains the Part A persistence designs, runnable file-based B1
implementation, B2 malformed-input tests, and JDBC source/schema for B2-B4.
MySQL-dependent execution remains blocked because the `mysql` client/server and
MySQL Connector/J are not available in the current environment. No database
timings or results have been fabricated.

## Structure

| Folder | Contents |
|---|---|
| `Task01_FileFormat` | A1 design |
| `Task02_BufferedIO` | A2 design |
| `Task03_DAODesign` | A3 design |
| `Task04_Serialization` | A4 design |
| `Task05_Transactions` | A5 design |
| `Task06_FileStoreImplementation` | B1 Java sources and actual output |
| `Task07_SQLInjection` | B2 local schema |
| `Task08_JDBC_Batch_Migration` | B3 loader and SQL schema |
| `Report` | Consolidated assignment report |

## Run B1

From `Assignment5`:

```powershell
New-Item -ItemType Directory -Force Task06_FileStoreImplementation\out
javac -d Task06_FileStoreImplementation\out Task06_FileStoreImplementation\src\*.java
java -cp Task06_FileStoreImplementation\out PassStore
java -cp Task06_FileStoreImplementation\out B1Runner
```

Expected `PassStore` output:

```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```

Actual output is saved in
`Task06_FileStoreImplementation/output/passstore_actual.txt`.

Run the B1 benchmark after compilation:

```powershell
java -cp Task06_FileStoreImplementation\out B1Benchmark
```

It generates 1 MiB, 10 MiB, and 50 MiB files, warms up each method, and
records the median of three runs. Search growth is measured at 1,000, 10,000,
and 100,000 records.

The verified medians were:

```text
sizeMiB,unbufferedMedianMs,bufferedMedianMs,lineCount
1,1396,1,17190
10,25001,21,171898
50,102387,30,859489

records,medianFindMs,found
1000,1,true
10000,5,true
100000,31,true
```

These values are machine-specific and are preserved in
`Task06_FileStoreImplementation/results/B1_benchmark_actual.csv`.

## Database setup when available

Set `DB_URL`, `DB_USER`, and `DB_PASS` in the process environment. Execute
`Task08_JDBC_Batch_Migration/sql/create_bus_pass.sql` only against the local
assignment database `csa09_a5_test`. Add MySQL Connector/J to the classpath
before compiling JDBC sources. Never commit the password.

## Status

Part A, B1 file experiments, B2 malformed-file handling, and B3/B4 JDBC source
and SQL artifacts are implemented. B2-B4 database execution and real JDBC
measurements remain blocked because MySQL and Connector/J are unavailable.
