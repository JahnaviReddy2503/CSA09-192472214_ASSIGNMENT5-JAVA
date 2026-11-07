# Task A1 - Design a Bus-Pass File Format (7 marks)

## Aim

Design a durable file representation for bus passes and algorithms for adding a
pass, finding a pass by ID, and listing passes by route. The design must tolerate
missing or damaged data without silently returning incorrect records.

Each logical record contains:

1. `passId`
2. `studentRegisterNumber`
3. `studentName`
4. `routeNumber`
5. `boardingStop`
6. `validityDate`
7. `amountPaid`

## A1.1 Record layouts

### Delimited text layout

One UTF-8 line represents one pass:

```text
passId|studentRegisterNumber|studentName|routeNumber|boardingStop|validityDate|amountPaid
```

Example:

```text
P1001|192311001|Arun Kumar|12|Central Station|2026-12-31|4500.00
```

The vertical bar (`|`) is the delimiter. The writer must escape a delimiter,
backslash, and line-break in a field before writing. A reversible convention is:

| Input character | Encoded form |
|---|---|
| `\` | `\\` |
| `|` | `\|` |
| line feed | `\n` |
| carriage return | `\r` |

The reader splits only on unescaped delimiters and reverses the escaping. A
field containing `|` is therefore preserved as one field. An alternative is to
reject delimiter characters during validation.

Text is variable length and human-readable. A newline terminates a record, so
records should not contain unescaped newline characters.

### Binary layout using `DataOutputStream`

The binary file contains a sequence of length-prefixed records. Every record is
written in this exact order:

| Field | Binary representation |
|---|---|
| magic/version | `writeInt(1)` |
| pass ID | `writeUTF(String)` |
| student register number | `writeUTF(String)` |
| student name | `writeUTF(String)` |
| route number | `writeInt(int)` |
| boarding stop | `writeUTF(String)` |
| validity date | `writeUTF(String)` in ISO-8601 `YYYY-MM-DD` form |
| amount paid | `writeDouble(double)` |

`writeUTF` stores a two-byte unsigned length followed by modified UTF-8 bytes.
The three integer-like fixed-width values occupy 4 bytes each, and the amount
occupies 8 bytes; strings have variable size plus their length prefix. The
record does not use a delimiter, so a name may contain `|`, commas, or other
punctuation without escaping. A name containing a line break is also safe in
the binary representation.

The reader uses the matching methods in the same order:
`readInt`, `readUTF`, `readUTF`, `readUTF`, `readInt`, `readUTF`, `readUTF`,
and `readDouble`. A version value allows a future reader to reject an unknown
layout rather than misinterpreting bytes.

### Storage workflow

```text
Input fields
    |
    +--> validate required fields and numeric/date ranges
    |
    +--> encode text fields / write binary fields
    |
    +--> append one complete record
    |
    +--> flush and close
```

## A1.2 Operations for the text design

### ALGORITHM AddBusPass

**INPUT:** `filePath`, `pass` containing the seven fields  
**OUTPUT:** one appended record, or an error report

1. BEGIN.
2. Validate that `passId`, register number, name, boarding stop, and validity
   date are non-empty.
3. Validate that route number is a positive integer and amount paid is finite
   and non-negative.
4. Escape delimiter, backslash, and line-break characters in every text field.
5. Convert the seven fields to one delimited record and append a line ending.
6. TRY:
   1. Open `FileWriter(filePath, true)`.
   2. Wrap it in `BufferedWriter`.
   3. Write the encoded record.
   4. Write the platform line separator.
   5. Flush the buffered writer.
   6. Close the writer using try-with-resources.
7. CATCH `IOException`:
   1. Report that the pass was not confirmed as stored.
   2. Preserve the existing file.
8. END TRY.
9. END.

`FileWriter` supplies character output and append mode; `BufferedWriter` reduces
small write operations and supplies `newLine()`. For a byte-defined encoding,
`FileOutputStream` can be wrapped in an `OutputStreamWriter` and then a
`BufferedWriter` so UTF-8 is explicit.

### ALGORITHM FindPassById

**INPUT:** `filePath`, `wantedPassId`  
**OUTPUT:** the matching pass, or `NOT FOUND`

1. BEGIN.
2. IF the file does not exist THEN return `NOT FOUND`.
3. TRY:
   1. Open `FileReader(filePath)`.
   2. Wrap it in `BufferedReader`.
   3. FOR each line until end-of-file:
      1. IF the line is blank THEN continue.
      2. TRY to parse the line using the escaped-delimiter parser.
      3. IF parsing fails or the number of fields is not seven THEN record a
         damaged-line warning and continue.
      4. IF the pass ID equals `wantedPassId` THEN return the decoded fields.
   4. END FOR.
   5. Return `NOT FOUND`.
4. CATCH `FileNotFoundException`:
   1. Treat the absent file as an empty store for a read operation.
   2. Return `NOT FOUND`.
5. CATCH other `IOException`:
   1. Report that the search could not be completed.
   2. Do not report `NOT FOUND` as if the read succeeded.
6. END TRY.
7. END.

`FileReader` reads characters and `BufferedReader` reads complete lines
efficiently. A byte-oriented implementation may use
`FileInputStream` plus an explicit UTF-8 decoder and `BufferedReader`.

### ALGORITHM ListPassesByRoute

**INPUT:** `filePath`, `wantedRoute`  
**OUTPUT:** all valid passes whose route number equals `wantedRoute`

1. BEGIN.
2. Create an empty result list.
3. IF the file does not exist THEN return the empty result list.
4. TRY:
   1. Open `FileReader(filePath)` and wrap it in `BufferedReader`.
   2. FOR each line until end-of-file:
      1. Skip blank lines.
      2. Parse and validate the line.
      3. IF the line is damaged THEN record a warning and continue.
      4. IF its route number equals `wantedRoute` THEN append the decoded pass
         to the result list.
   3. END FOR.
5. CATCH `IOException`:
   1. Report the read failure.
   2. Do not claim that the returned partial list is complete.
6. END TRY.
7. Return the result list.
8. END.

### Stream roles

```text
FileReader / FileInputStream
        |
        +--> connect the program to the file

BufferedReader / BufferedWriter
        |
        +--> reduce physical I/O calls and provide line-oriented operations

FileWriter / FileOutputStream
        |
        +--> write characters or bytes to the file
```

Character streams are appropriate for the UTF-8 text representation after an
explicit encoding choice. Byte streams are appropriate for binary records or
when the program must control raw bytes.

## A1.3 Damaged data and failure handling

### Damaged line

The parser checks the field count, escaping, required fields, route number,
date, and amount. A malformed record is logged with its line number and
skipped. A malformed line must not terminate a search or route listing, so a
later valid record remains discoverable. Invalid data is never silently
converted to a default value.

### Missing file

For `find` and `list`, a missing file is reported as an empty store only when
the application has explicitly chosen that read policy. For `add`, the parent
directory is created if permitted and the file is created in append mode. An
unexpected permission or I/O error is surfaced to the caller.

### Incomplete write after power failure

Appending directly to a text file can leave a final partial line. On the next
read, the final line fails validation and is skipped. For stronger durability:

1. Write the complete new content to a temporary file in the same directory.
2. Flush the writer.
3. Request a file-channel force to persist buffered bytes.
4. Atomically replace the old file with the temporary file.
5. Delete the temporary file after a failed replacement when safe.

The application can also add a record checksum or length prefix. A checksum
detects corruption; a temporary-file rename prevents a reader from observing
half of a rewritten file. A successful return from `add` means the application
completed its chosen durability steps, not merely that characters entered a
memory buffer.

## Alternative Concept and Complexity Analysis

### RandomAccessFile with fixed-length records

Define a fixed record size `R` bytes. Each field is stored in a fixed-width
region, with unused bytes padded. A record at zero-based index `i` begins at
byte offset `i × R`.

```text
record 0: [0, R)
record 1: [R, 2R)
record i: [iR, (i + 1)R)
```

If a separate index maps `passId` to `i`, the program can execute:

```text
ALGORITHM FindByIndexedId
INPUT: passId
1. Read record index for passId.
2. Open RandomAccessFile in read mode.
3. Seek to index * R.
4. Read and decode exactly one fixed-size record.
5. Verify the stored ID and return the record.
END
```

Without an index, finding by ID still scans records and is `O(n)`. With a
reliable ID-to-offset index, the seek and record read are `O(1)` relative to
the number of records. Listing a route remains `O(n)` unless a route index is
maintained. Random access is useful when records have stable offsets and
frequent point lookups are more important than human readability.

| Design | Add | Find by ID | List by route | Extra space |
|---|---:|---:|---:|---:|
| Delimited text, no index | `O(1)` amortized append | `O(n)` | `O(n)` | `O(1)` working space |
| Fixed records, no index | `O(1)` | `O(n)` | `O(n)` | padding up to `O(nR)` |
| Fixed records + ID index | `O(1)` plus index update | `O(1)` | `O(n)` | `O(n)` index + padding |

For both text scans, `n` is the number of records and `L` is the total file
size, so the more precise scan cost is `O(L)`. A fixed record wastes space when
values are shorter than their allocated fields, and a value longer than its
field must be rejected or truncated (truncation is unsafe). Text files are
portable and inspectable; fixed records provide predictable offsets but require
strict field sizing and index-recovery rules.

## Conclusion

Delimited text is appropriate for a small, inspectable bus-pass store when
validation, escaping, damaged-line handling, and an explicit durability policy
are implemented. A fixed-length `RandomAccessFile` design can provide constant
time point lookup with an index, but it trades portability and space efficiency
for predictable offsets.
