package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.util.CodeGenerator;

/** Generates sequential human-readable codes (EMP-0001, ...) inside the caller's transaction. */
public class CodeSequenceRepository extends JdbcSupport {

    public CodeSequenceRepository(DatabaseManager db) { super(db); }

    public String next(String prefix) {
        return db.inTransaction(c -> {
            long current = queryLong("SELECT next_value FROM code_sequences WHERE prefix = ?", prefix);
            if (current == 0) {
                current = 1;
                update("INSERT INTO code_sequences(prefix, next_value) VALUES (?, ?)", prefix, current + 1);
            } else {
                update("UPDATE code_sequences SET next_value = ? WHERE prefix = ?", current + 1, prefix);
            }
            return CodeGenerator.format(prefix, current);
        });
    }
}
