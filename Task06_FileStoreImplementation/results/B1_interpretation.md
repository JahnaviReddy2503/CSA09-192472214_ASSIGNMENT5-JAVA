# B1 Interpretation

## Text output evidence

Command:

```text
javac -d out src\*.java
java -cp out PassStore
```

Actual output:

```text
[P1003, 192311003, Divya, 7, 3800.0]
null
```

The damaged `P1002,DAMAGED LINE` line has two fields rather than five, so
`find` skips it and continues to the end. It does not stop the search and it
cannot be returned as a valid pass.

The line-counter smoke test also ran successfully:

```text
unbufferedLines=3, timeNanos=808001
bufferedLines=3, timeNanos=202000
```

These are actual single-run smoke-test timings, not the required median
1/10/50 MiB benchmark. They must not be used as the final performance table.

## Complexity

Sequential `find` is `O(n)` records and `O(1)` working memory. Loading the file
once into a `HashMap` costs `O(n)` preprocessing and `O(n)` memory, then each
of `q` searches is expected `O(1)`, giving `O(n + q)` rather than repeated
`O(qn)` scans.
