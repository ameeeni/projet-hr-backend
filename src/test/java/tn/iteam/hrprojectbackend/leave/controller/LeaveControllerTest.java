package tn.iteam.hrprojectbackend.leave.controller;

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
import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.dto.LeaveValidationDto;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.services.LeaveService;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveControllerTest {

    @Mock private LeaveService leaveService;
    @Mock private UserRepository userRepository;

    @InjectMocks private LeaveController leaveController;

    private LeaveResponseDto leaveResponseDto;
    private LeaveRequest leaveRequest;
    private User employee;

    @BeforeEach
    void setUp() {
        leaveResponseDto = LeaveResponseDto.builder()
                .id(1L).employeeId(10L).type(LeaveType.MALADIE).status(LeaveStatus.PENDING).build();

        leaveRequest = LeaveRequest.builder()
                .type(LeaveType.MALADIE)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(3))
                .build();

        employee = new User();
        employee.setId(10L);
        employee.setMatricule("EMP001");
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

    // ──────────── submit ────────────

    @Test
    void submit_ManagerRole_NoCheck_Returns201() {
        setupAuth("manager01", "ROLE_MANAGER");
        when(leaveService.submitRequest(10L, leaveRequest)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.submit(10L, leaveRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(leaveResponseDto, result.getBody());
    }

    @Test
    void submit_EmployeeRole_SameId_Returns201() {
        setupAuth("EMP001", "ROLE_EMPLOYEE");
        employee.setId(10L);
        when(userRepository.findByMatricule("EMP001")).thenReturn(Optional.of(employee));
        when(leaveService.submitRequest(10L, leaveRequest)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.submit(10L, leaveRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
    }

    @Test
    void submit_EmployeeRole_DifferentId_Returns403() {
        setupAuth("EMP001", "ROLE_EMPLOYEE");
        employee.setId(99L); // different from requested 10L
        when(userRepository.findByMatricule("EMP001")).thenReturn(Optional.of(employee));

        ResponseEntity<LeaveResponseDto> result = leaveController.submit(10L, leaveRequest);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void submit_EmployeeRole_UserNotFound_ThrowsException() {
        setupAuth("UNKNOWN", "ROLE_EMPLOYEE");
        when(userRepository.findByMatricule("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> leaveController.submit(10L, leaveRequest));
    }

    @Test
    void submit_HrRole_NoCheck_Returns201() {
        setupAuth("hr@test.com", "ROLE_HR");
        when(leaveService.submitRequest(10L, leaveRequest)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.submit(10L, leaveRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        verify(userRepository, never()).findByMatricule(any());
    }

    // ──────────── cancel ────────────

    @Test
    void cancel_Returns200() {
        when(leaveService.cancelRequest(1L, 10L)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.cancel(1L, 10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── validate ────────────

    @Test
    void validate_Returns200() {
        LeaveValidationDto dto = new LeaveValidationDto(LeaveStatus.APPROVED, "OK");
        when(leaveService.validateRequest(1L, 2L, dto)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.validate(1L, 2L, dto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getById ────────────

    @Test
    void getById_Returns200() {
        when(leaveService.getById(1L)).thenReturn(leaveResponseDto);

        ResponseEntity<LeaveResponseDto> result = leaveController.getById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getHistory ────────────

    @Test
    void getHistory_Returns200() {
        when(leaveService.getHistoryByEmployee(10L)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getHistory(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    // ──────────── getPendingByTeam ────────────

    @Test
    void getPendingByTeam_Returns200() {
        when(leaveService.getPendingByTeam(1L)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getPendingByTeam(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getAll ────────────

    @Test
    void getAll_Returns200() {
        when(leaveService.getAllRequests()).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getAll();

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getByStatus ────────────

    @Test
    void getByStatus_Returns200() {
        when(leaveService.getByStatus(LeaveStatus.PENDING)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getByStatus(LeaveStatus.PENDING);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    // ──────────── getMyRequests ────────────

    @Test
    void getMyRequests_ExactPrenomMatch_NoStatus_ReturnsHistory() {
        setupAuth("john.doe", "ROLE_EMPLOYEE");
        employee.setId(10L);
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.of(employee));
        when(leaveService.getHistoryByEmployee(10L)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getMyRequests(null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(leaveService).getHistoryByEmployee(10L);
    }

    @Test
    void getMyRequests_WithStatus_ReturnsFilteredList() {
        setupAuth("john.doe", "ROLE_EMPLOYEE");
        employee.setId(10L);
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.of(employee));
        when(leaveService.getByEmployeeAndStatus(10L, LeaveStatus.PENDING))
                .thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result =
                leaveController.getMyRequests(LeaveStatus.PENDING);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(leaveService).getByEmployeeAndStatus(10L, LeaveStatus.PENDING);
    }

    @Test
    void getMyRequests_FallbackToPartialMatch() {
        setupAuth("john.doe", "ROLE_EMPLOYEE");
        employee.setId(10L);
        when(userRepository.findByPrenomIgnoreCase("john")).thenReturn(Optional.empty());
        when(userRepository.findByPrenomContainingIgnoreCase("john")).thenReturn(List.of(employee));
        when(leaveService.getHistoryByEmployee(10L)).thenReturn(List.of());

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getMyRequests(null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getMyRequests_UserNotFound_ThrowsException() {
        setupAuth("unknown.user", "ROLE_EMPLOYEE");
        when(userRepository.findByPrenomIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByPrenomContainingIgnoreCase("unknown")).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> leaveController.getMyRequests(null));
    }

    // ──────────── getEmployeeRequests ────────────

    @Test
    void getEmployeeRequests_ManagerRole_NoCheck_ReturnsAll() {
        setupAuth("manager01", "ROLE_MANAGER");
        when(leaveService.getHistoryByEmployee(10L)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getEmployeeRequests(10L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(userRepository, never()).findByMatricule(any());
    }

    @Test
    void getEmployeeRequests_EmployeeRole_SameId_ReturnsOk() {
        setupAuth("EMP001", "ROLE_EMPLOYEE");
        employee.setId(10L);
        when(userRepository.findByMatricule("EMP001")).thenReturn(Optional.of(employee));
        when(leaveService.getHistoryByEmployee(10L)).thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getEmployeeRequests(10L, null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getEmployeeRequests_EmployeeRole_DifferentId_Returns403() {
        setupAuth("EMP001", "ROLE_EMPLOYEE");
        employee.setId(99L);
        when(userRepository.findByMatricule("EMP001")).thenReturn(Optional.of(employee));

        ResponseEntity<List<LeaveResponseDto>> result = leaveController.getEmployeeRequests(10L, null);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void getEmployeeRequests_WithStatus_ReturnsFiltered() {
        setupAuth("manager01", "ROLE_MANAGER");
        when(leaveService.getByEmployeeAndStatus(10L, LeaveStatus.PENDING))
                .thenReturn(List.of(leaveResponseDto));

        ResponseEntity<List<LeaveResponseDto>> result =
                leaveController.getEmployeeRequests(10L, LeaveStatus.PENDING);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(leaveService).getByEmployeeAndStatus(10L, LeaveStatus.PENDING);
    }
}
