package com.femzyk.fleetmanagement.service;

import com.femzyk.fleetmanagement.TestSupport;
import com.femzyk.fleetmanagement.config.AppConfig;
import com.femzyk.fleetmanagement.exception.AuthenticationException;
import com.femzyk.fleetmanagement.exception.AuthorizationException;
import com.femzyk.fleetmanagement.exception.BusinessRuleException;
import com.femzyk.fleetmanagement.exception.ValidationException;
import com.femzyk.fleetmanagement.model.Role;
import com.femzyk.fleetmanagement.model.User;
import com.femzyk.fleetmanagement.security.SessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    ServiceRegistry s;

    @BeforeEach void setUp() { s = TestSupport.freshRegistry(); }

    @Test void defaultAdminIsCreatedOnceAndMustChangePassword() {
        assertEquals(1, s.userRepository.count());
        assertFalse(s.auth.ensureDefaultAdministrator(), "second bootstrap must not create another admin");
        User u = TestSupport.loginAsAdmin(s);
        assertEquals(Role.ADMINISTRATOR, u.getRole());
        assertTrue(u.isMustChangePassword());
        assertNotEquals(AuthService.DEFAULT_ADMIN_PASSWORD, u.getPasswordHash(), "password must be hashed");
    }

    @Test void wrongPasswordIsRejectedAndLocksAfterLimit() {
        for (int i = 0; i < AppConfig.MAX_FAILED_LOGINS; i++) {
            assertThrows(AuthenticationException.class, () -> s.auth.login("admin", "wrong-pass-1".toCharArray()));
        }
        AuthenticationException e = assertThrows(AuthenticationException.class, () -> s.auth.login("admin", AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray()));
        assertTrue(e.getMessage().toLowerCase().contains("locked"));
    }

    @Test void unknownUserGetsGenericMessage() {
        AuthenticationException e = assertThrows(AuthenticationException.class, () -> s.auth.login("nobody", "whatever1".toCharArray()));
        assertEquals("Invalid username or password.", e.getMessage());
    }

    @Test void passwordPolicyEnforced() {
        TestSupport.loginAsAdmin(s);
        assertThrows(ValidationException.class, () -> s.auth.createUser("bob", "bob@x.com", "Bob", Role.VIEWER, "short".toCharArray()));
        assertThrows(ValidationException.class, () -> s.auth.createUser("bob", "bob@x.com", "Bob", Role.VIEWER, "onlyletters".toCharArray()));
        User bob = s.auth.createUser("bob", "bob@x.com", "Bob", Role.VIEWER, "Passw0rd!".toCharArray());
        assertTrue(bob.isMustChangePassword());
    }

    @Test void viewerCannotManageRecords() {
        TestSupport.loginAsAdmin(s);
        s.auth.createUser("view", "view@x.com", "Viewer", Role.VIEWER, "Viewer123".toCharArray());
        s.auth.logout();
        s.auth.login("view", "Viewer123".toCharArray());
        assertThrows(AuthorizationException.class, () -> Fixtures.employee(s, "A", "B"));
        assertThrows(AuthorizationException.class, () -> s.auth.listUsers());
        assertDoesNotThrow(() -> s.employees.findAll());
    }

    @Test void lastAdministratorCannotBeDeletedOrDemoted() {
        User admin = TestSupport.loginAsAdmin(s);
        assertThrows(BusinessRuleException.class, () -> s.auth.deleteUser(admin.getId()));
        assertThrows(BusinessRuleException.class, () -> s.auth.updateUser(admin.getId(), admin.getEmail(), admin.getFullName(), Role.VIEWER, true));
    }

    @Test void changeOwnPasswordClearsForcedFlagAndOldPasswordStopsWorking() {
        TestSupport.loginAsAdmin(s);
        s.auth.changeOwnPassword(AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray(), "NewSecret99".toCharArray());
        s.auth.logout();
        assertThrows(AuthenticationException.class, () -> s.auth.login("admin", AuthService.DEFAULT_ADMIN_PASSWORD.toCharArray()));
        User u = s.auth.login("admin", "NewSecret99".toCharArray());
        assertFalse(u.isMustChangePassword());
        assertTrue(SessionContext.currentUser().isPresent());
    }

    @Test void loginsAreAudited() {
        TestSupport.loginAsAdmin(s);
        assertTrue(s.auditLogRepository.search(null, "AUTH", "LOGIN", null, null, 10).size() >= 1);
    }
}
