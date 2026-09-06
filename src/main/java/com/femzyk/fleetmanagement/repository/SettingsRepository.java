package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;

import java.util.Optional;

public class SettingsRepository extends JdbcSupport {

    public SettingsRepository(DatabaseManager db) { super(db); }

    public Optional<String> get(String key) {
        return queryOne("SELECT value FROM app_settings WHERE key = ?", rs -> rs.getString(1), key);
    }

    public void put(String key, String value) {
        update("INSERT INTO app_settings(key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                key, value);
    }
}
