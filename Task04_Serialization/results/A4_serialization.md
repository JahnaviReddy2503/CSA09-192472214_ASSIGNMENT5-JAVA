# Task A4 - What Should Be Serialized? (4 marks)

## Aim

Persist a Hostel object graph without attempting to persist live operating-system
or database resources.

## A4.1 Object graph and transient fields

```text
Hostel
  +--> List<Room>
          +--> List<Resident>
                    +--> name
                    +--> regNo
                    +--> password
                    +--> transient Connection
```

`Hostel`, `Room`, and `Resident` implement `Serializable` if Java native
serialization is selected. The lists and ordinary value fields are serialized
recursively. `Connection` must be `transient`: it represents a live socket,
authentication state, and driver resource that cannot be meaningfully restored
from bytes. On deserialization, it is `null` and must be recreated through a
connection factory. In a real system, a password should not be serialized in
plain text; store a salted password hash or omit it.

## A4.2 Schema evolution

Adding `phoneNumber` changes the class layout. Old streams do not contain the
new field, so a compatible class can initialize it to Java's default (`null`)
or a migration default. Define an explicit `serialVersionUID` to control
compatibility. If the stored UID and class UID differ, Java throws
`InvalidClassException` rather than guessing. Custom `readObject` logic can
provide defaults and migrate older data.

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
