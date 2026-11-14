# B1 - File Store and Measurements (7 marks)

## B1.1 - File-Store Tests (2 marks)

The test cases and their observed pass/fail results are in
`output/test_results.txt`. They cover a valid lookup, a missing ID, a damaged
record between valid records, invalid route/amount fields, route listing, the
full seven-field format with escaped delimiters, and missing/empty files.

The provided five-field main method remains unchanged in behavior. Its actual
output is in `output/passstore_actual.txt`. The malformed P1002 row has two
fields, so it fails validation and the reader continues to later rows.

```text
add -> validate -> BufferedWriter -> file
find/list -> BufferedReader -> validate each row -> return valid match(es)
```

## B1.2 - Design Comparison (1 mark)

The implementation follows A1's streaming approach and uses try-with-resources.
The complete-code sample uses the required five legacy fields; an overload
stores all seven bus-pass fields with escaped pipe delimiters. This preserves
the expected output while representing boarding stop and validity date in the
full format. The B1 store skips invalid numeric/date fields instead of stopping
the scan.

## B1.3 - Buffering Measurements (2 marks)

`results/B1_benchmark_actual.csv` contains three measured runs and an average
for each exact-size log. It reports application-level read calls, including
the final EOF read: a 50 MiB byte counter makes about 52,428,801 calls and the
8 KiB counter about 6,401 calls. Those measurements match A2's prediction of
about one call per byte versus one call per buffer. Timings and line counts are
in the CSV; file reads were warmed before timing and are affected by the
operating-system cache.

| File size | Actual bytes | Unbuffered average | Buffered average | Unbuffered Java reads | Buffered Java reads |
|---:|---:|---:|---:|---:|---:|
| 1 MiB | 1,048,576 | 1,419.070 ms | 1.178 ms | 1,048,577 | 129 |
| 10 MiB | 10,485,760 | 15,462.941 ms | 11.066 ms | 10,485,761 | 1,281 |
| 50 MiB | 52,428,800 | 84,438.391 ms | 31.202 ms | 52,428,801 | 6,401 |

These are timings on the machine described in the README, not universal Java
performance guarantees. Read-call counts are instrumented Java method calls;
they are not operating-system syscall tracing. The same file was read by both
methods, results had matching line counts, and each timing is the mean of
three runs after a warm-up.

## B1.4 - Search Growth (2 marks)

The CSV contains three runs and an average for searching the final ID and a
missing ID in 1,000, 10,000, and 100,000 record files. Both cases scan through
the end of the file, so time should increase with the number of records. Use
the measured values in the CSV rather than extrapolating them.

| Records | Find last record average | Missing-ID average |
|---:|---:|---:|
| 1,000 | 2.684 ms | 2.802 ms |
| 10,000 | 12.311 ms | 9.926 ms |
| 100,000 | 29.171 ms | 31.264 ms |

The 10,000-record timings vary notably across their three repetitions, and
the increase is not proportional to record count. The implementation still
examines up to all `n` records, so the algorithmic cost is `O(n)`; JIT
optimization, filesystem caching, and fixed per-scan overhead make wall-clock
samples noisy at these sizes.

## Alternative Concept and Complexity Analysis

The `map_vs_scan` rows time 100 repeated end-of-file searches and include map
construction and repeated lookup time. Repeated scans use `O(qn)` work; the
map path uses expected `O(n + q)` work and `O(n)` memory. Its heap difference
is approximate because `System.gc()` and runtime object overhead make it an
estimate, not an exact allocation measurement.

| Records | 100 scans | Map build | 100 map lookups | Approximate map heap |
|---:|---:|---:|---:|---:|
| 1,000 | 55.516 ms | 1.978 ms | 0.006 ms | 867,336 bytes |
| 10,000 | 337.250 ms | 5.503 ms | 0.006 ms | 9,017,248 bytes |
| 100,000 | 3,073.974 ms | 87.969 ms | 0.027 ms | 46,130,384 bytes |

**Evidence Expected:** Functional tests (`output/test_results.txt`), complete
code output (`output/passstore_actual.txt`), buffer/growth/map measurements
(`results/B1_benchmark_actual.csv`), and interpretation (this file).
