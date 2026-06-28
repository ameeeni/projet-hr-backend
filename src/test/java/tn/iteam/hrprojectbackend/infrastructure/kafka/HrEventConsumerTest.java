package tn.iteam.hrprojectbackend.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.hrprojectbackend.dashboard.services.DashboardService;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Consumer.HrEventConsumer;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.events.LeaveSubmittedEvent;
import tn.iteam.hrprojectbackend.leave.events.LeaveValidatedEvent;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.events.EmployeeCreatedEvent;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrEventConsumerTest {

    @Mock private DashboardService dashboardService;
    @Mock private UserRepository userRepository;

    private HrEventConsumer consumer;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new HrEventConsumer(dashboardService, userRepository, objectMapper);
    }

    // ──────────── handleUserCreated ────────────

    @Test
    void handleUserCreated_IAMFormat_CreatesUser() {
        String message = """
                {
                  "username": "EMP001",
                  "lastName": "Doe",
                  "firstName": "John",
                  "email": "john@test.com",
                  "role": "EMPLOYEE",
                  "poste": "Developer",
                  "departement": "IT",
                  "soldeConge": 25
                }
                """;

        when(userRepository.existsByMatricule("EMP001")).thenReturn(false);
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("EMP001", savedUser.getMatricule());
        assertEquals("Doe", savedUser.getNom());
        assertEquals("John", savedUser.getPrenom());
        assertEquals("john@test.com", savedUser.getEmail());
        assertEquals(Role.EMPLOYEE, savedUser.getRole());
        assertEquals(25, savedUser.getSoldeConge());
    }

    @Test
    void handleUserCreated_HRFormat_CreatesUser() {
        String message = """
                {
                  "matricule": "HR001",
                  "nom": "Admin",
                  "prenom": "HR",
                  "email": "hr@test.com",
                  "role": "RH"
                }
                """;

        when(userRepository.existsByMatricule("HR001")).thenReturn(false);
        when(userRepository.existsByEmail("hr@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.HR, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_DuplicateMatricule_SkipsCreation() {
        String message = """
                {
                  "username": "EMP001",
                  "email": "john@test.com",
                  "role": "EMPLOYEE"
                }
                """;

        when(userRepository.existsByMatricule("EMP001")).thenReturn(true);

        consumer.handleUserCreated(message, "user-created");

        verify(userRepository, never()).save(any());
    }

    @Test
    void handleUserCreated_DuplicateEmail_SkipsCreation() {
        String message = """
                {
                  "username": "EMP002",
                  "email": "existing@test.com",
                  "role": "EMPLOYEE"
                }
                """;

        when(userRepository.existsByMatricule("EMP002")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        consumer.handleUserCreated(message, "user-created");

        verify(userRepository, never()).save(any());
    }

    @Test
    void handleUserCreated_WithCreatedAt_ParsesDate() {
        String message = """
                {
                  "username": "EMP003",
                  "email": "emp3@test.com",
                  "role": "MANAGER",
                  "createdAt": "2023-05-15T10:00:00"
                }
                """;

        when(userRepository.existsByMatricule("EMP003")).thenReturn(false);
        when(userRepository.existsByEmail("emp3@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(LocalDate.of(2023, 5, 15), captor.getValue().getDateEmbauche());
        assertEquals(Role.MANAGER, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_WithDateEmbauche_ParsesDate() {
        String message = """
                {
                  "matricule": "EMP004",
                  "email": "emp4@test.com",
                  "role": "ADMIN",
                  "dateEmbauche": "2022-01-01"
                }
                """;

        when(userRepository.existsByMatricule("EMP004")).thenReturn(false);
        when(userRepository.existsByEmail("emp4@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(LocalDate.of(2022, 1, 1), captor.getValue().getDateEmbauche());
        assertEquals(Role.HR, captor.getValue().getRole()); // ADMIN → HR
    }

    @Test
    void handleUserCreated_RoleHR_MapsCorrectly() {
        String message = """
                {
                  "username": "HR002",
                  "email": "hr2@test.com",
                  "role": "HR"
                }
                """;

        when(userRepository.existsByMatricule("HR002")).thenReturn(false);
        when(userRepository.existsByEmail("hr2@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.HR, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_RoleRoleManager_MapsCorrectly() {
        String message = """
                {
                  "username": "MGR001",
                  "email": "mgr@test.com",
                  "role": "ROLE_MANAGER"
                }
                """;

        when(userRepository.existsByMatricule("MGR001")).thenReturn(false);
        when(userRepository.existsByEmail("mgr@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.MANAGER, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_RoleEmploye_MapsCorrectly() {
        String message = """
                {
                  "username": "EMP005",
                  "email": "emp5@test.com",
                  "role": "EMPLOYE"
                }
                """;

        when(userRepository.existsByMatricule("EMP005")).thenReturn(false);
        when(userRepository.existsByEmail("emp5@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_UnknownRole_DefaultsToEmployee() {
        String message = """
                {
                  "username": "EMP006",
                  "email": "emp6@test.com",
                  "role": "UNKNOWN_ROLE"
                }
                """;

        when(userRepository.existsByMatricule("EMP006")).thenReturn(false);
        when(userRepository.existsByEmail("emp6@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_InvalidJson_DoesNotThrow() {
        String invalidJson = "not-a-json";

        // Should not throw, just log the error
        consumer.handleUserCreated(invalidJson, "user-created");

        verify(userRepository, never()).save(any());
    }

    @Test
    void handleUserCreated_NoRole_DefaultsToEmployee() {
        String message = """
                {
                  "username": "EMP007",
                  "email": "emp7@test.com"
                }
                """;

        when(userRepository.existsByMatricule("EMP007")).thenReturn(false);
        when(userRepository.existsByEmail("emp7@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(Role.EMPLOYEE, captor.getValue().getRole());
    }

    @Test
    void handleUserCreated_WithPassword_UsesProvided() {
        String message = """
                {
                  "username": "EMP008",
                  "email": "emp8@test.com",
                  "password": "mySecretPassword"
                }
                """;

        when(userRepository.existsByMatricule("EMP008")).thenReturn(false);
        when(userRepository.existsByEmail("emp8@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("mySecretPassword", captor.getValue().getPassword());
    }

    @Test
    void handleUserCreated_InvalidDateEmbauche_UsesCurrentDate() {
        String message = """
                {
                  "matricule": "EMP009",
                  "email": "emp9@test.com",
                  "dateEmbauche": "not-a-date"
                }
                """;

        when(userRepository.existsByMatricule("EMP009")).thenReturn(false);
        when(userRepository.existsByEmail("emp9@test.com")).thenReturn(false);

        consumer.handleUserCreated(message, "user-created");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        // Should use today's date as fallback
        assertEquals(LocalDate.now(), captor.getValue().getDateEmbauche());
    }

    // ──────────── onLeaveSubmitted ────────────

    @Test
    void onLeaveSubmitted_RefreshesDashboard() {
        LeaveSubmittedEvent event = LeaveSubmittedEvent.builder()
                .leaveRequestId(1L)
                .employeeNom("Doe")
                .build();

        consumer.onLeaveSubmitted(event);

        verify(dashboardService).refreshStats();
    }

    // ──────────── onLeaveValidated ────────────

    @Test
    void onLeaveValidated_RefreshesDashboard() {
        LeaveValidatedEvent event = LeaveValidatedEvent.builder()
                .leaveRequestId(1L)
                .decision(LeaveStatus.APPROVED)
                .build();

        consumer.onLeaveValidated(event);

        verify(dashboardService).refreshStats();
    }

    // ──────────── onEmployeeCreated ────────────

    @Test
    void onEmployeeCreated_RefreshesDashboard() {
        EmployeeCreatedEvent event = EmployeeCreatedEvent.builder()
                .employeeId(1L)
                .nom("Doe")
                .build();

        consumer.onEmployeeCreated(event);

        verify(dashboardService).refreshStats();
    }
}
