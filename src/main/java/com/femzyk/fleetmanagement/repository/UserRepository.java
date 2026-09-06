package com.femzyk.fleetmanagement.repository;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.database.JdbcSupport;
import com.femzyk.fleetmanagement.model.Role;
import com.femzyk.fleetmanagement.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class UserRepository extends JdbcSupport {

    private static final String COLUMNS = "id, username, email, full_name, password_salt, password_hash, role, active, "
            + "failed_login_attempts, locked_until, last_login_at, last_password_reset_at, must_change_password, "
            + "created_at, updated_at, deleted_at";

    public UserRepository(DatabaseManager db) { super(db); }

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setFullName(rs.getString("full_name"));
        u.setPasswordSalt(rs.getString("password_salt"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(bool(rs, "active"));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        u.setLockedUntil(dateTime(rs, "locked_until"));
        u.setLastLoginAt(dateTime(rs, "last_login_at"));
        u.setLastPasswordResetAt(dateTime(rs, "last_password_reset_at"));
        u.setMustChangePassword(bool(rs, "must_change_password"));
        u.setCreatedAt(dateTime(rs, "created_at"));
        u.setUpdatedAt(dateTime(rs, "updated_at"));
        u.setDeletedAt(dateTime(rs, "deleted_at"));
        return u;
    }

    public List<User> findAll() {
        return query("SELECT " + COLUMNS + " FROM users WHERE deleted_at IS NULL ORDER BY username", UserRepository::map);
    }

    public Optional<User> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", UserRepository::map, id);
    }

    public Optional<User> findByUsername(String username) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE username = ? COLLATE NOCASE AND deleted_at IS NULL",
                UserRepository::map, username);
    }

    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT " + COLUMNS + " FROM users WHERE email = ? COLLATE NOCASE AND deleted_at IS NULL",
                UserRepository::map, email);
    }

    public long count() {
        return queryLong("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL");
    }

    public long countByRole(Role role) {
        return queryLong("SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND active = 1 AND role = ?", role);
    }

    public User save(User u) {
        String now = now();
        if (u.isNew()) {
            long id = insert("INSERT INTO users(username, email, full_name, password_salt, password_hash, role, active, "
                    + "failed_login_attempts, locked_until, last_login_at, last_password_reset_at, must_change_password, created_at, updated_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    u.getUsername(), u.getEmail(), u.getFullName(), u.getPasswordSalt(), u.getPasswordHash(), u.getRole(),
                    u.isActive(), u.getFailedLoginAttempts(), u.getLockedUntil(), u.getLastLoginAt(), u.getLastPasswordResetAt(),
                    u.isMustChangePassword(), now, now);
            u.setId(id);
        } else {
            update("UPDATE users SET username=?, email=?, full_name=?, password_salt=?, password_hash=?, role=?, active=?, "
                    + "failed_login_attempts=?, locked_until=?, last_login_at=?, last_password_reset_at=?, must_change_password=?, updated_at=? "
                    + "WHERE id=?",
                    u.getUsername(), u.getEmail(), u.getFullName(), u.getPasswordSalt(), u.getPasswordHash(), u.getRole(),
                    u.isActive(), u.getFailedLoginAttempts(), u.getLockedUntil(), u.getLastLoginAt(), u.getLastPasswordResetAt(),
                    u.isMustChangePassword(), now, u.getId());
        }
        return u;
    }

    public void softDelete(long id) {
        update("UPDATE users SET deleted_at = ?, active = 0, updated_at = ? WHERE id = ?", now(), now(), id);
    }
}
