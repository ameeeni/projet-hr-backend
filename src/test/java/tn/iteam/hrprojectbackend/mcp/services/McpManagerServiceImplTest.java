package tn.iteam.hrprojectbackend.mcp.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.hrprojectbackend.leave.entities.Leave;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpManagerServiceImplTest {

    @Mock private LeaveRepository leaveRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private McpManagerServiceImpl mcpManagerService;

    private User employee;
    private Leave approvedLeave;
    private Leave pendingLeave;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setNom("Doe");
        employee.setPrenom("John");
        employee.setEmail("john@test.com");
        employee.setRole(Role.EMPLOYEE);
        employee.setSoldeConge(15);

        approvedLeave = Leave.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.APPROVED)
                .dateDebut(LocalDate.now().minusDays(5))
                .dateFin(LocalDate.now().minusDays(3))
                .nombreJours(3)
                .motif("Sick")
                .build();

        pendingLeave = Leave.builder()
                .id(2L)
                .employee(employee)
                .type(LeaveType.CONGE_ANNUEL)
                .status(LeaveStatus.PENDING)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(5))
                .nombreJours(5)
                .build();
    }

    // ──────────── getPendingRequestsByTeam ────────────

    @Test
    void getPendingRequestsByTeam_ReturnsList() {
        when(leaveRepository.findPendingByTeamId(1L)).thenReturn(List.of(pendingLeave));

        List<Map<String, Object>> result = mcpManagerService.getPendingRequestsByTeam(1L);

        assertThat(result).hasSize(1);
        assertEquals("Doe John", result.get(0).get("employee"));
        assertEquals("CONGE_ANNUEL", result.get(0).get("type"));
        assertEquals("PENDING", result.get(0).get("status"));
    }

    @Test
    void getPendingRequestsByTeam_EmptyList() {
        when(leaveRepository.findPendingByTeamId(1L)).thenReturn(List.of());

        List<Map<String, Object>> result = mcpManagerService.getPendingRequestsByTeam(1L);

        assertThat(result).isEmpty();
    }

    // ──────────── getTeamCalendar ────────────

    @Test
    void getTeamCalendar_ReturnsCalendarInfo() {
        when(leaveRepository.countActiveLeavesByTeamAndStatus(eq(1L), eq(LeaveStatus.APPROVED), any()))
                .thenReturn(3L);
        when(leaveRepository.countActiveLeavesByTeamTypeAndStatus(eq(1L), eq(LeaveType.TELETRAVAIL),
                eq(LeaveStatus.APPROVED), any()))
                .thenReturn(1L);

        Map<String, Object> result = mcpManagerService.getTeamCalendar(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("teamId"));
        assertEquals(3L, result.get("activeLeavesToday"));
        assertEquals(1L, result.get("teleworkingToday"));
    }

    @Test
    void getTeamCalendar_ZeroLeaves() {
        when(leaveRepository.countActiveLeavesByTeamAndStatus(any(), any(), any())).thenReturn(0L);
        when(leaveRepository.countActiveLeavesByTeamTypeAndStatus(any(), any(), any(), any())).thenReturn(0L);

        Map<String, Object> result = mcpManagerService.getTeamCalendar(2L);

        assertEquals(0L, result.get("activeLeavesToday"));
        assertEquals(0L, result.get("teleworkingToday"));
    }

    // ──────────── getMyLeaveBalance ────────────

    @Test
    void getMyLeaveBalance_ReturnsBalance() {
        Leave teletravailLeave = Leave.builder()
                .employee(employee)
                .type(LeaveType.TELETRAVAIL)
                .status(LeaveStatus.APPROVED)
                .nombreJours(2)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.TELETRAVAIL))
                .thenReturn(List.of(teletravailLeave));
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.MALADIE))
                .thenReturn(List.of(approvedLeave));

        Map<String, Object> result = mcpManagerService.getMyLeaveBalance(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("employeeId"));
        assertEquals("Doe John", result.get("nom"));
        assertEquals(15, result.get("soldeCongeRestant"));
        assertEquals(3L, result.get("joursMaladieUtilises"));
        assertEquals(2L, result.get("joursTeletravailUtilises"));
    }

    @Test
    void getMyLeaveBalance_NullSolde_UsesZero() {
        employee.setSoldeConge(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.TELETRAVAIL)).thenReturn(List.of());
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.MALADIE)).thenReturn(List.of());

        Map<String, Object> result = mcpManagerService.getMyLeaveBalance(1L);

        assertEquals(0, result.get("soldeCongeRestant"));
    }

    @Test
    void getMyLeaveBalance_NullNombreJours_HandlesGracefully() {
        Leave leaveWithNullDays = Leave.builder()
                .employee(employee)
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.APPROVED)
                .nombreJours(null) // null nombreJours
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.TELETRAVAIL)).thenReturn(List.of());
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.MALADIE))
                .thenReturn(List.of(leaveWithNullDays));

        Map<String, Object> result = mcpManagerService.getMyLeaveBalance(1L);

        assertEquals(0L, result.get("joursMaladieUtilises")); // null → 0
    }

    @Test
    void getMyLeaveBalance_EmployeeNotFound_ThrowsRuntimeException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> mcpManagerService.getMyLeaveBalance(99L));
    }

    @Test
    void getMyLeaveBalance_OnlyCountsApprovedLeaves() {
        Leave pendingMaladie = Leave.builder()
                .employee(employee)
                .type(LeaveType.MALADIE)
                .status(LeaveStatus.PENDING) // not approved → not counted
                .nombreJours(5)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.TELETRAVAIL)).thenReturn(List.of());
        when(leaveRepository.findByEmployeeIdAndType(1L, LeaveType.MALADIE))
                .thenReturn(List.of(pendingMaladie));

        Map<String, Object> result = mcpManagerService.getMyLeaveBalance(1L);

        assertEquals(0L, result.get("joursMaladieUtilises")); // pending not counted
    }

    // ──────────── getMyLeaveHistory ────────────

    @Test
    void getMyLeaveHistory_ReturnsList() {
        when(leaveRepository.findHistoryByEmployeeId(1L)).thenReturn(List.of(approvedLeave));

        List<Map<String, Object>> result = mcpManagerService.getMyLeaveHistory(1L);

        assertThat(result).hasSize(1);
        assertEquals("MALADIE", result.get(0).get("type"));
        assertEquals("APPROVED", result.get(0).get("status"));
        assertEquals(3, result.get(0).get("nombreJours"));
        assertEquals("Sick", result.get(0).get("motif"));
    }

    @Test
    void getMyLeaveHistory_NullNombreJoursAndMotif_HandlesGracefully() {
        Leave leaveWithNulls = Leave.builder()
                .employee(employee)
                .type(LeaveType.CONGE_ANNUEL)
                .status(LeaveStatus.CANCELLED)
                .dateDebut(LocalDate.now().minusDays(10))
                .dateFin(LocalDate.now().minusDays(8))
                .nombreJours(null)
                .motif(null) // null motif
                .build();

        when(leaveRepository.findHistoryByEmployeeId(1L)).thenReturn(List.of(leaveWithNulls));

        List<Map<String, Object>> result = mcpManagerService.getMyLeaveHistory(1L);

        assertThat(result).hasSize(1);
        assertEquals(0, result.get(0).get("nombreJours")); // null → 0
        assertEquals("", result.get(0).get("motif")); // null → ""
    }

    @Test
    void getMyLeaveHistory_EmptyList() {
        when(leaveRepository.findHistoryByEmployeeId(1L)).thenReturn(List.of());

        List<Map<String, Object>> result = mcpManagerService.getMyLeaveHistory(1L);

        assertThat(result).isEmpty();
    }
}
