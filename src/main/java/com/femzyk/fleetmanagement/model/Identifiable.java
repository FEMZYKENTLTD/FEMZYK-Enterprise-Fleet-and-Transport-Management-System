package com.femzyk.fleetmanagement.model;

/** Anything persisted with a numeric surrogate key. */
public interface Identifiable {
    Long getId();
    void setId(Long id);
    default boolean isNew() { return getId() == null; }
}
