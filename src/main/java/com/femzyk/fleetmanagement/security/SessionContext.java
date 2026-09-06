package com.femzyk.fleetmanagement.security;

import com.femzyk.fleetmanagement.exception.AuthorizationException;
import com.femzyk.fleetmanagement.model.Permission;
import com.femzyk.fleetmanagement.model.User;

import java.util.Optional;

/** Holds the currently authenticated user for the application process. */
public final class SessionContext {

    private static volatile User currentUser;

    private SessionContext() {}

    public static void login(User user) { currentUser = user; }
    public static void logout() { currentUser = null; }

    public static Optional<User> currentUser() { return Optional.ofNullable(currentUser); }

    public static String currentUsername() {
        User u = currentUser;
        return u == null ? "system" : u.getUsername();
    }

    public static boolean has(Permission permission) {
        User u = currentUser;
        return u != null && u.hasPermission(permission);
    }

    /** Service-layer guard. Throws when nobody is logged in or the role lacks the permission. */
    public static void require(Permission permission) {
        User u = currentUser;
        if (u == null) {
            throw new AuthorizationException("You must be logged in to perform this action.");
        }
        if (!u.hasPermission(permission)) {
            throw new AuthorizationException("Your role (" + u.getRole().getDisplayName()
                    + ") does not permit this action: " + permission.name().replace('_', ' ').toLowerCase() + ".");
        }
    }
}
