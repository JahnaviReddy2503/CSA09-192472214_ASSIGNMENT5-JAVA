# Task A5 - Correctness and Safety (4 marks)

## A5.1 Transfer transaction

**ALGORITHM TransferWalletAmount**

**INPUT:** database connection, hostel wallet ID, canteen wallet ID  
**OUTPUT:** committed transfer or failure

1. BEGIN.
2. Set `amount` to `500`.
3. Set `autoCommit` to `false`.
4. TRY:
   1. Execute a parameterized debit:
      `UPDATE wallet SET balance = balance - ? WHERE wallet_id = ? AND balance >= ?`.
   2. IF the affected row count is not one THEN throw an insufficient-funds error.
   3. Execute a parameterized credit:
      `UPDATE wallet SET balance = balance + ? WHERE wallet_id = ?`.
   4. IF the affected row count is not one THEN throw a missing-wallet error.
   5. Commit the transaction.
5. CATCH any database or validation error:
   1. Roll back.
   2. Report failure.
6. FINALLY restore the original auto-commit setting.
7. END.

Both updates become visible together or neither becomes visible. The debit
condition prevents a negative balance. A database constraint and suitable
locking/isolation level also prevent concurrent transfers from overwriting one
another.

```text
BEGIN -> debit hostel -> credit canteen -> COMMIT
             | error at either step
             +-------------------------> ROLLBACK
```

## A5.2 Injection and prepared statements

The unsafe query is:

```text
SELECT * FROM users WHERE id='inputId' AND pwd='inputPassword'
```

If an attacker supplies an ID such as `' OR '1'='1` and the input is inserted
without validation, the resulting expression can make the predicate true for
many rows. The input changes SQL syntax instead of remaining data.

The safe design is:

```text
PREPARE SELECT * FROM users WHERE id = ? AND pwd = ?
BIND id as parameter 1
BIND password as parameter 2
EXECUTE
```

`PreparedStatement` sends the SQL structure separately from values, so quotes
and operators in the input are compared as literal characters. Passwords
should normally be verified against a salted hash rather than compared as
plain text.

## Alternative Concept and Complexity Analysis

Two independent auto-commit updates can debit the hostel and then fail before
crediting the canteen, creating or destroying money. A transaction adds
coordination and logging overhead, but each transfer still has constant
application work and two database updates: `O(1)` time for one transfer and
`O(1)` working space. The correctness risk of separate statements is
unacceptable for financial data. Escaping quotes or regular-expression checks
are weaker because validation is easy to miss and cannot safely model every SQL
syntax/context; parameter binding is the database-supported boundary.

## Conclusion

Use one transaction for the debit and credit and use `PreparedStatement` for
every user-controlled value. Correctness and injection resistance are
properties of the database interaction, not merely of a GUI validation rule.
