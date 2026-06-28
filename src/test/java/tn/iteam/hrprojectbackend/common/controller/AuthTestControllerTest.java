package tn.iteam.hrprojectbackend.common.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tn.iteam.hrprojectbackend.leave.services.LeaveService;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthTestControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private LeaveService leaveService;

    @InjectMocks private AuthTestController authTestController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuth(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, authorities));
    }

    // ──────────── me() ────────────

    @Test
    void me_WithAuthentication_ReturnsUserInfo() {
        setupAuth("john@test.com", "ROLE_EMPLOYEE");

        ResponseEntity<Map<String, Object>> result = authTestController.me();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("john@test.com", result.getBody().get("username"));
        assertTrue((Boolean) result.getBody().get("authenticated"));
        assertNotNull(result.getBody().get("authorities"));
    }

    @Test
    void me_WithMultipleRoles_ReturnsAllAuthorities() {
        setupAuth("hr@test.com", "ROLE_HR", "HR_READ");

        ResponseEntity<Map<String, Object>> result = authTestController.me();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) result.getBody().get("authorities");
        assertTrue(authorities.contains("ROLE_HR"));
    }

    @Test
    void me_NoAuthentication_ReturnsError() {
        SecurityContextHolder.clearContext();

        ResponseEntity<Map<String, Object>> result = authTestController.me();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue(result.getBody().containsKey("error"));
    }

    // ──────────── checkOwner() ────────────

    @Test
    void checkOwner_IsOwner_UserFound() {
        setupAuth("john@test.com", "ROLE_EMPLOYEE");

        User user = new User();
        user.setId(1L);
        user.setEmail("john@test.com");
        user.setRole(Role.EMPLOYEE);

        when(leaveService.isOwner("john@test.com", 1L)).thenReturn(true);
        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> result = authTestController.checkOwner(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertTrue((Boolean) result.getBody().get("isOwner"));
        assertTrue((Boolean) result.getBody().get("userFound"));
        assertEquals(1L, result.getBody().get("userId"));
        assertTrue((Boolean) result.getBody().get("wouldBeAllowed")); // isEmployee=true, isOwner=true → true
    }

    @Test
    void checkOwner_IsEmployee_NotOwner_NotAllowed() {
        setupAuth("other@test.com", "ROLE_EMPLOYEE");

        when(leaveService.isOwner("other@test.com", 1L)).thenReturn(false);
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> result = authTestController.checkOwner(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertFalse((Boolean) result.getBody().get("isOwner"));
        assertFalse((Boolean) result.getBody().get("userFound"));
        assertFalse((Boolean) result.getBody().get("wouldBeAllowed")); // isEmployee=true, isOwner=false → false
    }

    @Test
    void checkOwner_IsManager_AlwaysAllowed() {
        setupAuth("manager@test.com", "ROLE_MANAGER");

        when(leaveService.isOwner("manager@test.com", 5L)).thenReturn(false);
        when(userRepository.findByEmail("manager@test.com")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> result = authTestController.checkOwner(5L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue((Boolean) result.getBody().get("wouldBeAllowed")); // isEmployee=false → true regardless
    }

    @Test
    void checkOwner_NoAuthentication_ReturnsError() {
        SecurityContextHolder.clearContext();

        ResponseEntity<Map<String, Object>> result = authTestController.checkOwner(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().containsKey("error"));
    }

    // ──────────── listUsers() ────────────

    @Test
    void listUsers_ReturnsList() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("emp1@test.com");
        user1.setMatricule("EMP001");
        user1.setNom("Doe");
        user1.setPrenom("John");
        user1.setRole(Role.EMPLOYEE);

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("mgr@test.com");
        user2.setMatricule("MGR001");
        user2.setNom("Manager");
        user2.setPrenom("Bob");
        user2.setRole(Role.MANAGER);

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        ResponseEntity<List<Map<String, Object>>> result = authTestController.listUsers();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertEquals("emp1@test.com", result.getBody().get(0).get("email"));
        assertEquals("EMP001", result.getBody().get(0).get("matricule"));
        assertEquals("EMPLOYEE", result.getBody().get(0).get("role"));
    }

    @Test
    void listUsers_EmptyList_ReturnsEmpty() {
        when(userRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> result = authTestController.listUsers();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().isEmpty());
    }
}
