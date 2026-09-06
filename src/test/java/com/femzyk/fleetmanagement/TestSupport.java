package com.femzyk.fleetmanagement;

import com.femzyk.fleetmanagement.database.DatabaseManager;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.security.SessionContext;
import com.femzyk.fleetmanagement.service.AuthService;
import com.femzyk.fleetmanagement.service.ServiceRegistry;

/** Shared test fixture: fresh in-memory database + services, logged in as the default administrator. */
public final class TestSupport {

    private TestSupport() {}

    public static ServiceRegistry freshRegistry() {
        DatabaseManager db = DatabaseManager.inMemory();
        ServiceRegistry r = new ServiceRegistry(db);
        r.bootstrap();
        return r;
    }

    public static ServiceRegistry freshRegistryLoggedInAsAdmin() {
        ServiceRegistry r = freshRegistry();
        loginAsAdmin(r);
        return r;
    }

    public static User loginAsAdmin(ServiceRegistry r) {
        SessionContext.logout();
        return r.auth.login(AuthService.DEFAULT_ADMIN_USERNAME, AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray());
    }
}
