# Part A - Cross-Task Complexity Analysis

The variables below make the cost statements comparable: `n` is the number of
records, `L` is the number of bytes processed, `B` is the I/O buffer size, `q`
is the number of searches, `b` is the JDBC batch size, and `m` is the length of
an input value. These are algorithmic costs; database network and disk latency
are measured separately in Part B where MySQL is available.

| Design | Time | Additional space | Engineering implication |
|---|---:|---:|---|
| Delimited-file find/list | `O(L)` scan, equivalently `O(n)` records | `O(1)` streaming, excluding returned list | Simple and inspectable; repeated searches repeat the scan |
| Fixed-width RandomAccessFile, no index | `O(n)` find / route list | `O(nR)` storage including padding | Stable offsets alone do not find an arbitrary ID |
| Fixed-width file plus ID-to-offset index | Expected `O(1)` point lookup | `O(n)` index plus `O(nR)` data | Requires index rebuild/recovery and bounded field sizes |
| Byte-by-byte line counter | `O(L)` CPU and `L + 1` Java `read()` calls including EOF | `O(1)` | Excessive call overhead |
| Buffered line counter | `O(L)` CPU and about `ceil(L/B) + 1` bulk reads | `O(B)` | Same scan work with fewer reads |
| `Files.readAllLines` | `O(L)` | `O(L)` decoded strings and object overhead | Useful only when all lines need to stay in memory |
| DAO keyed lookup | One indexed database lookup, generally `O(1)` in row count | `O(1)` for one result | Centralizes SQL and row mapping |
| DAO full list | `O(n)` | `O(n)` if materialized | Stream/page results for larger tables |
| Text/serialization save and load | `O(n)` values | `O(1)` for streaming, `O(n)` for a full object graph | Serialization has Java-specific schema compatibility |
| JDBC one-row insert | `O(1)` row work plus one round trip | `O(1)` | Clear fit for an individual counter operation |
| JDBC batch of `n` rows | `O(n)` row work and about `ceil(n/b)` round trips | `O(b)` batch values | Bounded memory and fewer round trips |
| Repeated file search for `q` IDs | `O(qn)` records scanned | `O(1)` | Becomes costly as both `q` and `n` grow |
| HashMap file index | `O(n + q)` expected | `O(n)` | One load cost, then expected constant-time lookups |
| Prepared JDBC values | `O(m)` binding for an input | `O(m)` | Values remain data, not SQL syntax |

The measured B1 timings are machine-specific and are in
`Task06_FileStoreImplementation/results/B1_benchmark_actual.csv`; the
interpretation is in
`Task06_FileStoreImplementation/results/B1_interpretation.md`. MySQL timing
evidence is unavailable on this machine, so no database timing is used in this
table.

**Evidence Expected:** Per-task derivations and assumptions are in the
Alternative Concept and Complexity Analysis section of each A1-A5 document;
measured file-store data is in the B1 benchmark CSV.
