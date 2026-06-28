package tn.iteam.hrprojectbackend.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tn.iteam.hrprojectbackend.common.exception.BusinessException;
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.mapper.UserMapper;
import tn.iteam.hrprojectbackend.users.repository.TeamRepository;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserMapper userMapper;
    @Mock private HrEventProducer hrEventProducer;

    @InjectMocks private UserServiceImpl userService;

    private UserRequest validRequest;
    private User employee;
    private Team team;
    private User manager;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(10L);
        team.setNom("Dev Team");
        team.setMembres(new ArrayList<>());

        manager = new User();
        manager.setId(2L);
        manager.setNom("Manager");
        manager.setEmail("manager@test.com");
        manager.setRole(Role.MANAGER);

        employee = new User();
        employee.setId(1L);
        employee.setMatricule("EMP001");
        employee.setNom("Doe");
        employee.setPrenom("John");
        employee.setEmail("john.doe@test.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setSoldeConge(20);
        employee.setTeam(team);

        validRequest = UserRequest.builder()
                .matricule("EMP001")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@test.com")
                .password("password123")
                .role(Role.EMPLOYEE)
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .matricule("EMP001")
                .nom("Doe")
                .prenom("John")
                .email("john.doe@test.com")
                .role(Role.EMPLOYEE)
                .build();
    }

    // ──────────── createUser ────────────

    @Test
    void createUser_Success() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userMapper.toEntity(any())).thenReturn(employee);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.createUser(validRequest);

        assertNotNull(result);
        assertEquals("EMP001", result.getMatricule());
        verify(hrEventProducer).sendEmployeeCreated(any());
        verify(userRepository).save(any());
    }

    @Test
    void createUser_WithTeam_Success() {
        validRequest.setTeamId(10L);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userMapper.toEntity(any())).thenReturn(employee);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.createUser(validRequest);

        assertNotNull(result);
        verify(teamRepository).findById(10L);
    }

    @Test
    void createUser_WithManager_Success() {
        validRequest.setManagerId(2L);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userMapper.toEntity(any())).thenReturn(employee);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.createUser(validRequest);

        assertNotNull(result);
        verify(userRepository).findById(2L);
    }

    @Test
    void createUser_DuplicateEmail_ThrowsBusinessException() {
        when(userRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(validRequest));
        assertEquals("DUPLICATE_EMAIL", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_DuplicateMatricule_ThrowsBusinessException() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule("EMP001")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(validRequest));
        assertEquals("DUPLICATE_MATRICULE", ex.getCode());
    }

    @Test
    void createUser_TeamNotFound_ThrowsResourceNotFoundException() {
        validRequest.setTeamId(99L);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.createUser(validRequest));
    }

    @Test
    void createUser_ManagerNotFound_ThrowsResourceNotFoundException() {
        validRequest.setManagerId(99L);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.createUser(validRequest));
    }

    @Test
    void createUser_HrWithTeam_ThrowsBusinessException() {
        validRequest.setRole(Role.HR);
        validRequest.setTeamId(10L);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.createUser(validRequest));
        assertEquals("HR_CANNOT_HAVE_TEAM", ex.getCode());
    }

    // ──────────── getUserById ────────────

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ──────────── getUserByUsername ────────────

    @Test
    void getUserByUsername_Success() {
        when(userRepository.findByUsername("EMP001")).thenReturn(Optional.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        UserResponse result = userService.getUserByUsername("EMP001");

        assertNotNull(result);
        assertEquals("EMP001", result.getMatricule());
    }

    @Test
    void getUserByUsername_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findByUsername("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername("UNKNOWN"));
    }

    // ──────────── getAllUsers ────────────

    @Test
    void getAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllUsers_EmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // ──────────── getUsersByRole ────────────

    @Test
    void getUsersByRole_ReturnsList() {
        when(userRepository.findByRole(Role.EMPLOYEE)).thenReturn(List.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        List<UserResponse> result = userService.getUsersByRole(Role.EMPLOYEE);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUsersByRole_EmptyList() {
        when(userRepository.findByRole(Role.HR)).thenReturn(List.of());

        List<UserResponse> result = userService.getUsersByRole(Role.HR);

        assertThat(result).isEmpty();
    }

    // ──────────── getUsersByTeam ────────────

    @Test
    void getUsersByTeam_Success() {
        when(teamRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findByTeamId(10L)).thenReturn(List.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        List<UserResponse> result = userService.getUsersByTeam(10L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUsersByTeam_TeamNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.getUsersByTeam(99L));
    }

    @Test
    void getUsersByTeam_EmptyList() {
        when(teamRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findByTeamId(10L)).thenReturn(List.of());

        List<UserResponse> result = userService.getUsersByTeam(10L);

        assertThat(result).isEmpty();
    }

    // ──────────── updateUser ────────────

    @Test
    void updateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, validRequest);

        assertNotNull(result);
        verify(userRepository).save(employee);
    }

    @Test
    void updateUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(99L, validRequest));
    }

    @Test
    void updateUser_DuplicateEmail_ThrowsBusinessException() {
        employee.setEmail("old@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail("john.doe@test.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(1L, validRequest));
        assertEquals("DUPLICATE_EMAIL", ex.getCode());
    }

    @Test
    void updateUser_SameEmail_NoCheck() {
        employee.setEmail("john.doe@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, validRequest);

        assertNotNull(result);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void updateUser_DuplicateMatricule_ThrowsBusinessException() {
        employee.setMatricule("OLD001");
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule("EMP001")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(1L, validRequest));
        assertEquals("DUPLICATE_MATRICULE", ex.getCode());
    }

    @Test
    void updateUser_HrWithTeam_ThrowsBusinessException() {
        validRequest.setRole(Role.HR);
        validRequest.setTeamId(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateUser(1L, validRequest));
        assertEquals("HR_CANNOT_HAVE_TEAM", ex.getCode());
    }

    @Test
    void updateUser_WithTeam_Success() {
        validRequest.setTeamId(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.updateUser(1L, validRequest);

        assertNotNull(result);
        verify(teamRepository).findById(10L);
    }

    @Test
    void updateUser_TeamNotFound_ThrowsResourceNotFoundException() {
        validRequest.setTeamId(99L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(1L, validRequest));
    }

    @Test
    void updateUser_NullTeam_SetsTeamNull() {
        validRequest.setTeamId(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        userService.updateUser(1L, validRequest);

        assertNull(employee.getTeam());
    }

    @Test
    void updateUser_WithManager_Success() {
        validRequest.setManagerId(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        userService.updateUser(1L, validRequest);

        assertEquals(manager, employee.getManager());
    }

    @Test
    void updateUser_ManagerNotFound_ThrowsResourceNotFoundException() {
        validRequest.setManagerId(99L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUser(1L, validRequest));
    }

    @Test
    void updateUser_NullManager_SetsManagerNull() {
        validRequest.setManagerId(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMatricule(any())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        userService.updateUser(1L, validRequest);

        assertNull(employee.getManager());
    }

    // ──────────── deleteUser ────────────

    @Test
    void deleteUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository).delete(employee);
    }

    @Test
    void deleteUser_NotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).delete(any());
    }

    // ──────────── assignToTeam ────────────

    @Test
    void assignToTeam_Success() {
        employee.setTeam(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.assignToTeam(1L, 10L);

        assertNotNull(result);
        assertEquals(team, employee.getTeam());
    }

    @Test
    void assignToTeam_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.assignToTeam(99L, 10L));
    }

    @Test
    void assignToTeam_TeamNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.assignToTeam(1L, 99L));
    }

    @Test
    void assignToTeam_HrRole_ThrowsBusinessException() {
        employee.setRole(Role.HR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.assignToTeam(1L, 10L));
        assertEquals("HR_CANNOT_HAVE_TEAM", ex.getCode());
    }

    @Test
    void assignToTeam_AlreadyInTeam_ThrowsBusinessException() {
        employee.setTeam(team); // already in this team
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.assignToTeam(1L, 10L));
        assertEquals("USER_ALREADY_IN_TEAM", ex.getCode());
    }

    // ──────────── assignManager ────────────

    @Test
    void assignManager_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.assignManager(1L, 2L);

        assertNotNull(result);
        assertEquals(manager, employee.getManager());
    }

    @Test
    void assignManager_WithHrRole_Success() {
        User hrUser = new User();
        hrUser.setId(3L);
        hrUser.setRole(Role.HR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(3L)).thenReturn(Optional.of(hrUser));
        when(userRepository.save(any())).thenReturn(employee);
        when(userMapper.toResponse(any())).thenReturn(userResponse);

        UserResponse result = userService.assignManager(1L, 3L);

        assertNotNull(result);
    }

    @Test
    void assignManager_UserNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.assignManager(99L, 2L));
    }

    @Test
    void assignManager_ManagerNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.assignManager(1L, 99L));
    }

    @Test
    void assignManager_InvalidManagerRole_ThrowsBusinessException() {
        User notManager = new User();
        notManager.setId(3L);
        notManager.setRole(Role.EMPLOYEE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(3L)).thenReturn(Optional.of(notManager));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.assignManager(1L, 3L));
        assertEquals("INVALID_MANAGER_ROLE", ex.getCode());
    }

    @Test
    void assignManager_SelfManager_ThrowsBusinessException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Use a manager that is the same user
        User selfManager = new User();
        selfManager.setId(1L);
        selfManager.setRole(Role.MANAGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(selfManager));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.assignManager(1L, 1L));
        assertEquals("SELF_MANAGER_NOT_ALLOWED", ex.getCode());
    }
}
