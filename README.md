# CSA09 Programming in Java - Assignment 5

Student name: **[NOT PROVIDED]**  
Register number: **[NOT PROVIDED]**  
Repository link: **[NOT CREATED - GitHub CLI is unavailable]**  
Java tested: OpenJDK 17.0.21

This workspace contains the Part A persistence designs and the runnable,
file-based B1 implementation. MySQL-dependent B2-B4 execution is blocked
because the `mysql` client/server and MySQL Connector/J are not available in
the current environment. No database timings or results have been fabricated.

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

## Database setup when available

Set `DB_URL`, `DB_USER`, and `DB_PASS` in the process environment. Execute
`Task08_JDBC_Batch_Migration/sql/create_bus_pass.sql` only against the local
assignment database `csa09_a5_test`. Add MySQL Connector/J to the classpath
before compiling JDBC sources. Never commit the password.

## Status

Part A and B1 have been implemented and verified. B2-B4 database execution,
real JDBC measurements, personal details, task-wise commits, and GitHub push
remain pending because the required local services, dependency, identity
values, and authenticated GitHub tooling are unavailable.
