package com.femzyk.fleetmanagement.model;

import java.time.LocalDateTime;

/** Immutable audit trail entry. Written by services; never editable through the UI. */
public class AuditLog {

    private Long id;
    private LocalDateTime timestamp;
    private String username;
    private String action;      // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, RESTORE, ASSIGN, ...
    private String module;      // EMPLOYEE, DRIVER, VEHICLE, ...
    private String reference;   // record code or id
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
