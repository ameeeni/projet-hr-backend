package tn.iteam.hrprojectbackend.common.security;

import org.junit.jupiter.api.Test;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    private User buildUser(Long id, String email, Role role, Team team) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setTeam(team);
        return user;
    }

    @Test
    void from_WithTeam_SetsTeamId() {
        Team team = new Team();
        team.setId(10L);

        User user = buildUser(1L, "john@test.com", Role.EMPLOYEE, team);
        UserPrincipal principal = UserPrincipal.from(user);

        assertEquals(1L, principal.getId());
        assertEquals("john@test.com", principal.getEmail());
        assertEquals(Role.EMPLOYEE, principal.getRole());
        assertEquals(10L, principal.getTeamId());
    }

    @Test
    void from_WithoutTeam_NullTeamId() {
        User user = buildUser(2L, "hr@test.com", Role.HR, null);
        UserPrincipal principal = UserPrincipal.from(user);

        assertEquals(2L, principal.getId());
        assertEquals("hr@test.com", principal.getEmail());
        assertEquals(Role.HR, principal.getRole());
        assertNull(principal.getTeamId());
    }

    @Test
    void getAuthorities_ReturnsRoleAuthority() {
        User user = buildUser(1L, "mgr@test.com", Role.MANAGER, null);
        UserPrincipal principal = UserPrincipal.from(user);

        var authorities = principal.getAuthorities();

        assertEquals(1, authorities.size());
        assertTrue(authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    void getAuthorities_HrRole() {
        User user = buildUser(1L, "hr@test.com", Role.HR, null);
        UserPrincipal principal = UserPrincipal.from(user);

        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR")));
    }

    @Test
    void getUsername_ReturnsEmail() {
        User user = buildUser(1L, "john@test.com", Role.EMPLOYEE, null);
        UserPrincipal principal = UserPrincipal.from(user);

        assertEquals("john@test.com", principal.getUsername());
    }

    @Test
    void getPassword_ReturnsEmpty() {
        User user = buildUser(1L, "john@test.com", Role.EMPLOYEE, null);
        UserPrincipal principal = UserPrincipal.from(user);

        assertEquals("", principal.getPassword());
    }

    @Test
    void accountDetails_AllReturnTrue() {
        User user = buildUser(1L, "john@test.com", Role.EMPLOYEE, null);
        UserPrincipal principal = UserPrincipal.from(user);

        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }

    @Test
    void directConstructor_SetsAllFields() {
        UserPrincipal principal = new UserPrincipal(5L, "test@test.com", Role.HR, 20L);

        assertEquals(5L, principal.getId());
        assertEquals("test@test.com", principal.getEmail());
        assertEquals(Role.HR, principal.getRole());
        assertEquals(20L, principal.getTeamId());
    }
}
