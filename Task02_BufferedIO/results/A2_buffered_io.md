# Task A2 - Do Not Read What You Do Not Need (5 marks)

## Aim

Count line terminators in a 50 MiB log while avoiding one operating-system read
operation for every byte.

## A2.1 One-byte design

If `50 MiB` means `50 × 1024 × 1024` bytes, the loop makes exactly
`52,428,800` calls to `FileInputStream.read()` if the file is that size. If the
assignment uses decimal megabytes, the count is `50,000,000`.

The call count is proportional to the file size: `O(L)` calls for `L` bytes.
Although the JVM and OS may cache some data, each Java call still has method,
buffer, and boundary-check overhead. If the implementation reaches the OS for
each byte, system-call overhead makes it especially slow.

## A2.2 Buffered redesign

**ALGORITHM CountLinesBuffered**

**INPUT:** path to a byte log  
**OUTPUT:** number of line-feed bytes

1. BEGIN.
2. Set `count` to zero.
3. Allocate a byte array `buffer` of 8192 bytes.
4. TRY:
   1. Open `FileInputStream`.
   2. Wrap it in `BufferedInputStream`.
   3. REPEAT while `read(buffer)` returns a positive number `used`.
      1. FOR each index from 0 through `used - 1`:
         1. IF `buffer[index]` is line feed THEN increment `count`.
      2. END FOR.
   4. END REPEAT.
5. CATCH `IOException`: report the path and rethrow the error.
6. Return `count`.
7. END.

With an 8192-byte buffer, the program performs approximately
`ceil(L / 8192)` stream reads instead of `L` one-byte reads. The byte scan is
still `O(L)` CPU time, but the number of I/O requests is reduced by roughly a
factor of 8192. Working memory is `O(B)`, where `B` is the buffer size.

```text
FileInputStream -> BufferedInputStream -> byte[8192] -> line counter
```

An unbuffered `byte[]` loop using `FileInputStream.read(buffer)` is also valid;
`BufferedInputStream` adds an internal buffer and is useful when callers request
small reads.

## A2.3 When buffering helps little

Buffering gives little benefit when the input is already fully resident in
memory or is an interactive source that produces only one byte at a time and
must be processed immediately. It also cannot remove latency caused by a remote
service that delivers data slowly. Buffering may increase latency when a program
needs each byte immediately and the buffer is not flushed or consumed promptly.

## Alternative Concept and Complexity Analysis

`Files.readAllLines(path)` is convenient when the complete file is needed for
random access or repeated processing:

| Method | Time | Extra space | Suitable use |
|---|---:|---:|---|
| Buffered stream | `O(L)` | `O(B)` | Large files and streaming |
| `Files.readAllLines` | `O(L)` | `O(L)` | Small/medium files needed in memory |

`readAllLines` stores every decoded line and creates string objects, so the
real memory use can exceed the raw file size. For a 50 MiB log, streaming is
safer because it retains only the buffer and counters. Both approaches must
validate the selected character encoding when the log is text rather than raw
ASCII/UTF-8 bytes.

## Conclusion

Buffered streaming preserves linear processing time while changing the
physical-read count from approximately one per byte to one per buffer. It is
the default design for large logs; `readAllLines` is appropriate only when the
whole file is intentionally required in memory.
