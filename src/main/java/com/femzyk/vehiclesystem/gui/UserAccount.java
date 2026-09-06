package com.femzyk.vehiclesystem.gui;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Legacy shadow class for deserialising v4.x users.dat files. Field names/SUID must not change. */
public class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String email;
    private String passwordSalt;
    private String passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastPasswordResetAt;

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordSalt() { return passwordSalt; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getLastPasswordResetAt() { return lastPasswordResetAt; }
}
