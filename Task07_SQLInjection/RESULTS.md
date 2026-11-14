# B2 - Security and Damaged-File Results (5 marks)

## B2.1 - Unsafe and Prepared Login

`UnsafeLogin` builds the SQL by concatenation. `SecureLogin` binds both values
through placeholders. `SqlInjectionRunner` exercises the valid-account,
invalid-password, and injected-input cases against only the local
`transport_db` database; `DatabaseConnection` rejects non-local URLs and other
schemas.

The test account is deliberately synthetic (`alice`). `sql/schema.sql` creates
the table, and the runner seeds the account with a disposable
`A5_TEST_PASSWORD` value. It is not a real account. The exact injected input is
`' OR '1'='1' -- ` for the ID with a wrong password. The unsafe SQL is printed
by the program; the prepared SQL remains `SELECT 1 FROM users WHERE id = ? AND
pwd = ?`.

**Execution status:** The source compiles, but database authentication was not
executed. This machine has no MySQL client or service, port `127.0.0.1:3306` is
unreachable, and `config/db.properties` is not present. Therefore this report
does not claim observed unsafe acceptance or prepared rejection. See
`results/injection_blocked_actual.txt` for the real local connection error.

| Test input | Unsafe concatenation | PreparedStatement | Actual execution |
|---|---|---|---|
| Valid synthetic `alice` account | Accept | Accept | Blocked by missing local DB configuration |
| Wrong password | Reject | Reject | Blocked by missing local DB configuration |
| ID `' OR '1'='1' -- ` and wrong password | Bypass expected | Reject expected | Blocked by missing local DB configuration |

## B2.2 - Damaged Pass-File Validation

| Input case | Actual result | Evidence |
|---|---|---|
| Valid 5-field row | Accepted | `results/file_adversarial_actual.txt` |
| Wrong field count | Rejected: expected 5 legacy or 7 full fields, found 4 | Same output |
| Non-numeric amount | Rejected: amount is not numeric | Same output |
| Empty line | Rejected: empty line | Same output |
| Valid row after damaged rows | Found by PassStore | Same output |

These inputs show why a field-count-only file reader is incomplete: malformed
numeric data can also reach the application unless fields are parsed and
validated before use.

## Alternative Concept and Complexity Analysis

| Method | Benefit | Weakness |
|---|---|---|
| Backslash quote escaping | Small linear-time transformation | Depends on SQL dialect and SQL mode; every value and query context must be escaped correctly |
| Regular-expression allowlist | Can reject unexpected identifier syntax | Rejects legitimate values such as `O'Reilly`; it does not separate SQL syntax from values |
| `PreparedStatement` | Parameters remain values, independent of quoting rules | Requires parameterized JDBC calls |

For example, the backslash escaper in `AlternativeValidation` changes
`O'Reilly` to a backslash-quoted string. Under MySQL's `NO_BACKSLASH_ESCAPES`
mode that value no longer has the assumed quoting meaning and can become
invalid SQL. The same legitimate name is rejected by the allowlist. This shows
the dialect dependence and false-rejection risk without sending an exploit to
any non-local server.

Escaping and regular-expression checks each take `O(m)` time and `O(m)` output
space for an input of length `m`. Prepared binding also processes the input in
`O(m)` time but avoids the manually maintained SQL-escaping boundary. The
timing program in `src/AlternativeValidation.java` compares local query
execution methods when MySQL is configured. No comparison values are reported
because the database is unavailable.

```text
local test DB -> execute unsafe and prepared controls -> print SQL and outcomes
                    |
                    +-- injected data changes query only in unsafe statement
                    +-- prepared placeholders keep injected text as data
```

**Evidence Expected:** Login implementation and local schema
(`src/UnsafeLogin.java`, `src/SecureLogin.java`, `sql/schema.sql`); actual file
validation (`results/file_adversarial_actual.txt`); database blocker
(`results/injection_blocked_actual.txt`); comparison and complexity
(Alternative Concept and Complexity Analysis).
