package tn.iteam.hrprojectbackend.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.iteam.hrprojectbackend.users.entities.Role;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuthentication(Long userId, String email, Role role, Long teamId) {
        UserPrincipal principal = new UserPrincipal(userId, email, role, teamId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getCurrentUser_ReturnsAuthenticatedUser() {
        setupAuthentication(1L, "john@test.com", Role.EMPLOYEE, 10L);

        UserPrincipal result = SecurityUtils.getCurrentUser();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@test.com", result.getEmail());
    }

    @Test
    void getCurrentUser_NoAuthentication_ThrowsIllegalStateException() {
        SecurityContextHolder.clearContext();

        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUser);
    }

    @Test
    void getCurrentUser_NotAuthenticated_ThrowsIllegalStateException() {
        // Set a non-authenticated token
        SecurityContextHolder.getContext().setAuthentication(null);

        assertThrows(IllegalStateException.class, SecurityUtils::getCurrentUser);
    }

    @Test
    void getCurrentUserId_ReturnsCorrectId() {
        setupAuthentication(42L, "user@test.com", Role.MANAGER, null);

        Long result = SecurityUtils.getCurrentUserId();

        assertEquals(42L, result);
    }

    @Test
    void getCurrentUserTeamId_ReturnsCorrectTeamId() {
        setupAuthentication(1L, "user@test.com", Role.EMPLOYEE, 99L);

        Long result = SecurityUtils.getCurrentUserTeamId();

        assertEquals(99L, result);
    }

    @Test
    void getCurrentUserTeamId_NullTeam_ReturnsNull() {
        setupAuthentication(1L, "hr@test.com", Role.HR, null);

        Long result = SecurityUtils.getCurrentUserTeamId();

        assertNull(result);
    }
}
