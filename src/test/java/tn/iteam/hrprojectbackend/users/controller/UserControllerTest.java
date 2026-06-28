package tn.iteam.hrprojectbackend.users.controller;

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
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.mapper.UserMapper;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;
import tn.iteam.hrprojectbackend.users.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private HrEventProducer hrEventProducer;
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserController userController;

    private UserResponse userResponse;
    private User user;

    @BeforeEach
    void setUp() {
        userResponse = UserResponse.builder()
                .id(1L).nom("Doe").prenom("John").email("john@test.com").role(Role.EMPLOYEE).build();

        user = new User();
        user.setId(1L);
        user.setNom("Doe");
        user.setPrenom("John");
        user.setEmail("john@test.com");
        user.setRole(Role.EMPLOYEE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupAuth(String username, String... roles) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createUser_Returns201() {
        UserRequest request = UserRequest.builder()
                .matricule("EMP001").nom("Doe").prenom("John")
                .email("john@test.com").password("pass").role(Role.EMPLOYEE).build();
        when(userService.createUser(request)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.createUser(request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(userResponse, result.getBody());
    }

    @Test
    void getById_Returns200() {
        when(userService.getUserById(1L)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.getById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(userResponse, result.getBody());
    }

    @Test
    void getAll_Returns200() {
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));

        ResponseEntity<List<UserResponse>> result = userController.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void getByRole_Returns200() {
        when(userService.getUsersByRole(Role.EMPLOYEE)).thenReturn(List.of(userResponse));

        ResponseEntity<List<UserResponse>> result = userController.getByRole(Role.EMPLOYEE);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getByTeam_Returns200() {
        when(userService.getUsersByTeam(1L)).thenReturn(List.of(userResponse));

        ResponseEntity<List<UserResponse>> result = userController.getByTeam(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void updateUser_Returns200() {
        UserRequest request = UserRequest.builder()
                .matricule("EMP001").nom("Doe").prenom("John")
                .email("john@test.com").password("pass").role(Role.EMPLOYEE).build();
        when(userService.updateUser(1L, request)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.updateUser(1L, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void deleteUser_Returns204() {
        doNothing().when(userService).deleteUser(1L);

        ResponseEntity<Void> result = userController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void assignToTeam_Returns200() {
        when(userService.assignToTeam(1L, 2L)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.assignToTeam(1L, 2L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void assignManager_Returns200() {
        when(userService.assignManager(1L, 2L)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.assignManager(1L, 2L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getCurrentUser ────────────

    @Test
    void getCurrentUser_ExactPrenomMatch_ReturnsUser() {
        setupAuth("john.doe", "ROLE_EMPLOYEE");
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.getCurrentUser();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository).findByPrenomIgnoreCase("john");
    }

    @Test
    void getCurrentUser_FallbackToPartialMatch_ReturnsUser() {
        setupAuth("john.doe", "ROLE_EMPLOYEE");
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.empty());
        when(userRepository.findByPrenomContainingIgnoreCase("john")).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.getCurrentUser();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository).findByPrenomContainingIgnoreCase("john");
    }

    @Test
    void getCurrentUser_NotFound_ThrowsResourceNotFoundException() {
        setupAuth("unknown.user", "ROLE_EMPLOYEE");
        when(userRepository.findByPrenomIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByPrenomContainingIgnoreCase("unknown")).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> userController.getCurrentUser());
    }

    @Test
    void getCurrentUser_UsernameWithoutDot_UsesFullName() {
        setupAuth("john", "ROLE_EMPLOYEE");
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);

        ResponseEntity<UserResponse> result = userController.getCurrentUser();

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
