package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.audit.AuditService;
import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.exception.AuthenticationException;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.RecordNotFoundException;
import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.model.Role;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.repository.UserRepository;
import com.femzyk.fleetmanagement.security.PasswordUtil;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.validation.Validators;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Authentication and user administration.
 *
 * <p>Evolved from the original UserDatabase: same PBKDF2 hashing, now with roles, account lockout after
 * repeated failures, disabled accounts, forced password change and an audit trail.</p>
 */
public class AuthService {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "Admin@2024";

    private final UserRepository users;
    private final AuditService audit;

    public AuthService(UserRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    /** First-run bootstrap: creates the default administrator when no users exist. Idempotent. */
    public boolean ensureDefaultAdministrator() {
        if (users.count() > 0) {
            return false;
        }
        User admin = new User();
        admin.setUsername(DEFAULT_ADMIN_USERNAME);
        admin.setEmail("admin@femzyk.local");
        admin.setFullName("System Administrator");
        admin.setRole(Role.ADMINISTRATOR);
        admin.setMustChangePassword(true);
        setPassword(admin, DEFAULT_ADMIN_PASSWORD.toCharArray());
        users.save(admin);
        audit.record("system", "BOOTSTRAP", "USER", admin.getUsername(), "Default administrator account created");
        return true;
    }

    public User login(String username, char[] password) {
        if (Validators.isBlank(username) || password == null || password.length == 0) {
            throw new AuthenticationException("Username and password are required.");
        }
        User user = users.findByUsername(username.trim()).orElse(null);
        if (user == null) {
            audit.record(username.trim(), "LOGIN_FAILED", "AUTH", username.trim(), "Unknown username");
            throw new AuthenticationException("Invalid username or password.");
        }
        if (!user.isActive()) {
            audit.record(user.getUsername(), "LOGIN_FAILED", "AUTH", user.getUsername(), "Account disabled");
            throw new AuthenticationException("This account has been disabled. Contact an administrator.");
        }
        if (user.isLocked()) {
            audit.record(user.getUsername(), "LOGIN_FAILED", "AUTH", user.getUsername(), "Account locked");
            throw new AuthenticationException("Account is temporarily locked after too many failed attempts. Try again later.");
        }
        boolean ok = PasswordUtil.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash());
        Arrays.fill(password, '\0');
        if (!ok) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= AppConfig.MAX_FAILED_LOGINS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(AppConfig.LOCKOUT_MINUTES));
                user.setFailedLoginAttempts(0);
            }
            users.save(user);
            audit.record(user.getUsername(), "LOGIN_FAILED", "AUTH", user.getUsername(), "Incorrect password (attempt " + attempts + ")");
            throw new AuthenticationException("Invalid username or password.");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        users.save(user);
        SessionContext.login(user);
        audit.record(user.getUsername(), "LOGIN", "AUTH", user.getUsername(), "Successful login as " + user.getRole());
        return user;
    }

    public void logout() {
        SessionContext.currentUser().ifPresent(u -> audit.record(u.getUsername(), "LOGOUT", "AUTH", u.getUsername(), "User logged out"));
        SessionContext.logout();
    }

    public List<User> listUsers() {
        SessionContext.require(Permission.MANAGE_USERS);
        return users.findAll();
    }

    public User createUser(String username, String email, String fullName, Role role, char[] password) {
        SessionContext.require(Permission.MANAGE_USERS);
        validateUser(username, email, fullName, role);
        String policy = PasswordUtil.checkPolicy(password);
        if (policy != null) throw new ValidationException(policy);
        User u = new User();
        u.setUsername(username.trim());
        u.setEmail(email.trim().toLowerCase());
        u.setFullName(fullName.trim());
        u.setRole(role);
        u.setMustChangePassword(true);
        setPassword(u, password);
        users.save(u);
        audit.record("CREATE", "USER", u.getUsername(), "User created with role " + role);
        return u;
    }

    public User updateUser(long id, String email, String fullName, Role role, boolean active) {
        SessionContext.require(Permission.MANAGE_USERS);
        User u = users.findById(id).orElseThrow(() -> new RecordNotFoundException("User", id));
        validateUser(u.getUsername(), email, fullName, role);
        if (u.getRole() == Role.ADMINISTRATOR && (role != Role.ADMINISTRATOR || !active) && users.countByRole(Role.ADMINISTRATOR) <= 1) {
            throw new BusinessRuleException("At least one active administrator must remain.");
        }
        u.setEmail(email.trim().toLowerCase());
        u.setFullName(fullName.trim());
        u.setRole(role);
        u.setActive(active);
        if (active) { u.setLockedUntil(null); u.setFailedLoginAttempts(0); }
        users.save(u);
        audit.record("UPDATE", "USER", u.getUsername(), "User updated: role=" + role + ", active=" + active);
        return u;
    }

    public void deleteUser(long id) {
        SessionContext.require(Permission.MANAGE_USERS);
        User u = users.findById(id).orElseThrow(() -> new RecordNotFoundException("User", id));
        if (SessionContext.currentUser().map(c -> c.getId().equals(id)).orElse(false)) {
            throw new BusinessRuleException("You cannot delete the account you are logged in with.");
        }
        if (u.getRole() == Role.ADMINISTRATOR && users.countByRole(Role.ADMINISTRATOR) <= 1) {
            throw new BusinessRuleException("At least one active administrator must remain.");
        }
        users.softDelete(id);
        audit.record("DELETE", "USER", u.getUsername(), "User account deactivated and removed");
    }

    /** Administrator resets another user's password. */
    public void adminResetPassword(long id, char[] newPassword) {
        SessionContext.require(Permission.MANAGE_USERS);
        User u = users.findById(id).orElseThrow(() -> new RecordNotFoundException("User", id));
        String policy = PasswordUtil.checkPolicy(newPassword);
        if (policy != null) throw new ValidationException(policy);
        setPassword(u, newPassword);
        u.setMustChangePassword(true);
        u.setLockedUntil(null);
        u.setFailedLoginAttempts(0);
        users.save(u);
        audit.record("PASSWORD_RESET", "USER", u.getUsername(), "Password reset by administrator");
    }

    /** Self-service reset (login screen): the user must know username + registered e-mail. */
    public void resetPasswordWithEmail(String username, String email, char[] newPassword) {
        User u = users.findByUsername(username == null ? "" : username.trim()).orElse(null);
        if (u == null || u.getEmail() == null || !u.getEmail().equalsIgnoreCase(email == null ? "" : email.trim())) {
            audit.record(username, "PASSWORD_RESET_FAILED", "AUTH", username, "Username/e-mail did not match");
            throw new AuthenticationException("Username and e-mail address do not match any account.");
        }
        if (!u.isActive()) throw new AuthenticationException("This account has been disabled.");
        String policy = PasswordUtil.checkPolicy(newPassword);
        if (policy != null) throw new ValidationException(policy);
        setPassword(u, newPassword);
        u.setMustChangePassword(false);
        u.setLockedUntil(null);
        u.setFailedLoginAttempts(0);
        users.save(u);
        audit.record(u.getUsername(), "PASSWORD_RESET", "AUTH", u.getUsername(), "Password reset via registered e-mail");
    }

    /** Logged-in user changes own password. */
    public void changeOwnPassword(char[] currentPassword, char[] newPassword) {
        User u = SessionContext.currentUser().orElseThrow(() -> new AuthenticationException("Not logged in."));
        if (!PasswordUtil.verifyPassword(currentPassword, u.getPasswordSalt(), u.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect.");
        }
        String policy = PasswordUtil.checkPolicy(newPassword);
        if (policy != null) throw new ValidationException(policy);
        setPassword(u, newPassword);
        u.setMustChangePassword(false);
        users.save(u);
        audit.record("PASSWORD_CHANGE", "USER", u.getUsername(), "Password changed by user");
    }

    private void validateUser(String username, String email, String fullName, Role role) {
        Validators.errors()
                .required(username, "Username")
                .check(Validators.isValidUsername(username), "Username must be 3-32 characters (letters, digits, . _ -).")
                .email(email, "E-mail", true)
                .required(fullName, "Full name")
                .required(role, "Role")
                .throwIfAny();
    }

    private static void setPassword(User u, char[] password) {
        String salt = PasswordUtil.generateSalt();
        u.setPasswordSalt(salt);
        u.setPasswordHash(PasswordUtil.hashPassword(password, salt));
        u.setLastPasswordResetAt(LocalDateTime.now());
        Arrays.fill(password, '\0');
    }
}
