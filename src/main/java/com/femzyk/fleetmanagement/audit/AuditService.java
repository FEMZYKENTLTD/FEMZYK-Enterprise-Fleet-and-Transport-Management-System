package com.femzyk.fleetmanagement.audit;

import com.femzyk.fleetmanagement.model.AuditLog;
import com.femzyk.fleetmanagement.repository.AuditLogRepository;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.util.AppLogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Records significant actions. Inspired by the Course Management System's SystemLogger (single point of
 * entry, consistent format) but persisted to the {@code audit_logs} table.
 */
public class AuditService {

    private static final Logger LOG = AppLogger.get(AuditService.class);
    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void record(String action, String module, String reference, String description) {
        record(SessionContext.currentUsername(), action, module, reference, description);
    }

    public void record(String username, String action, String module, String reference, String description) {
        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setUsername(username == null ? "system" : username);
        entry.setAction(action);
        entry.setModule(module);
        entry.setReference(reference);
        entry.setDescription(description);
        try {
            repository.append(entry);
        } catch (RuntimeException e) {
            // Auditing must never break the business operation; log the failure technically.
            LOG.log(Level.SEVERE, "Audit write failed: " + action + " " + module + " " + reference, e);
        }
    }

    public List<AuditLog> recent(int limit) { return repository.findRecent(limit); }

    public List<AuditLog> search(String text, String module, String action, LocalDate from, LocalDate to, int limit) {
        return repository.search(text, module, action, from, to, limit);
    }

    public long count() { return repository.count(); }
    public List<String> modules() { return repository.distinctModules(); }
    public List<String> actions() { return repository.distinctActions(); }
}
