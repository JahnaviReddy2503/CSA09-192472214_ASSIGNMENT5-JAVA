# Task A4 - What Should Be Serialized? (4 marks)

## Aim

Persist a Hostel object graph without attempting to persist live operating-system
or database resources.

## A4.1 - Object Graph and Transient Fields (2 marks)

The compilable demonstration is in [`Resident.java`](../src/Resident.java) and
[`SerializationDemo.java`](../src/SerializationDemo.java). It confirms that a
transient connection is `null` after deserialization.

```text
Hostel
  +--> List<Room>
          +--> List<Resident>
                    +--> name
                    +--> regNo
                    +--> password
                    +--> transient Connection
```

```text
Hostel object -> ObjectOutputStream -> file
file -> ObjectInputStream -> Hostel object -> reconnect live resources
```

`Hostel`, `Room`, and `Resident` implement `Serializable` if Java native
serialization is selected. The lists and ordinary value fields are serialized
recursively; each reachable object is written once and shared references are
preserved. A non-serializable, non-transient reachable field causes
`NotSerializableException`. `Connection` must be `transient`: it represents a live socket,
authentication state, and driver resource that cannot be meaningfully restored
from bytes. On deserialization, it is `null` and must be recreated through a
connection factory. In a real system, a password should not be serialized in
plain text; store a salted password hash or omit it.

## A4.2 - Schema Evolution (2 marks)

Adding `phoneNumber` changes the class layout. Old streams do not contain the
new field, so with an unchanged explicit `serialVersionUID` the new reference
field is `null` (and numeric fields are zero) unless custom `readObject` logic
provides a migration default. If the UID changes, or an automatically computed
UID changes after the class change, Java throws `InvalidClassException` rather
than guessing. Declare `serialVersionUID` explicitly and version migrations.

## Alternative Concept and Complexity Analysis

For `n` residents:

| Format | Size | Save/load time | New fields |
|---|---:|---:|---|
| Java serialization | compact-ish but class metadata included | `O(n)` | Java-specific compatibility rules |
| CSV | usually smallest for simple fields | `O(n)` | add a column and migration/default |
| JSON | more readable, larger due to names | `O(n)` | optional fields and versioning are explicit |

All three require `O(n)` output/input work. Streaming CSV/JSON can use `O(1)`
resident memory; loading the complete graph uses `O(n)`. Native serialization
is convenient inside one Java version but is unsuitable as an interchange
format and should not deserialize untrusted data.

## Conclusion

Serialize durable values and identifiers, not live connections or secrets.
CSV/JSON with explicit versioning are preferable for long-lived or
cross-language data; native serialization is acceptable only in controlled,
trusted Java environments.

**Evidence Expected:** Object graph, transient-resource explanation, and
serialization workflow (A4.1); UID and added-field compatibility behavior
(A4.2); format and complexity comparison (Alternative Concept and Complexity
Analysis).
