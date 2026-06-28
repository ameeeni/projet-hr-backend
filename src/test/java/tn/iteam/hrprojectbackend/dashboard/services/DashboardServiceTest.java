package tn.iteam.hrprojectbackend.dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.hrprojectbackend.dashboard.dto.DashboardStatsDto;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.repository.TeamRepository;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private LeaveRepository leaveRepository;

    @InjectMocks private DashboardService dashboardService;

    private Team team;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);
        team.setNom("Dev Team");
        team.setMembres(new ArrayList<>());
    }

    private void mockRepositoryCalls() {
        when(userRepository.countByRole(Role.EMPLOYEE)).thenReturn(10L);
        when(userRepository.countByRole(Role.MANAGER)).thenReturn(2L);
        when(userRepository.countByRole(Role.HR)).thenReturn(1L);
        when(userRepository.averageSoldeConge()).thenReturn(18.5);
        when(teamRepository.count()).thenReturn(3L);
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamRepository.countMembersByTeamId(1L)).thenReturn(5L);
        when(leaveRepository.countByTeamIdAndStatus(eq(1L), eq(LeaveStatus.PENDING))).thenReturn(2L);
        when(leaveRepository.countByStatus(LeaveStatus.PENDING)).thenReturn(5L);
        when(leaveRepository.countByStatus(LeaveStatus.APPROVED)).thenReturn(10L);
        when(leaveRepository.countByStatus(LeaveStatus.REJECTED)).thenReturn(3L);
        when(leaveRepository.countByStatus(LeaveStatus.CANCELLED)).thenReturn(1L);
        when(leaveRepository.countByStatusAndType(LeaveStatus.PENDING, LeaveType.CONGE_ANNUEL)).thenReturn(2L);
        when(leaveRepository.countByStatusAndType(LeaveStatus.PENDING, LeaveType.MALADIE)).thenReturn(2L);
        when(leaveRepository.countByStatusAndType(LeaveStatus.PENDING, LeaveType.TELETRAVAIL)).thenReturn(1L);
    }

    // ──────────── getStats ────────────

    @Test
    void getStats_FirstCall_ComputesStats() {
        mockRepositoryCalls();

        DashboardStatsDto result = dashboardService.getStats();

        assertNotNull(result);
        assertEquals(10L, result.getTotalEmployes());
        assertEquals(2L, result.getTotalManagers());
        assertEquals(1L, result.getTotalHR());
        assertEquals(3L, result.getTotalEquipes());
        assertEquals(5L, result.getDemandesPending());
        assertEquals(10L, result.getDemandesApprouveesTotal());
        assertEquals(3L, result.getDemandesRefuseesTotal());
        assertEquals(1L, result.getDemandesAnnuleesTotal());
        assertEquals(2L, result.getCongesPending());
        assertEquals(2L, result.getMaladiePending());
        assertEquals(1L, result.getTeletravailPending());
        assertEquals(18.5, result.getSoldeMoyenConge());
        assertThat(result.getStatsParEquipe()).hasSize(1);
        assertEquals("Dev Team", result.getStatsParEquipe().get(0).getTeamNom());
        assertEquals(5L, result.getStatsParEquipe().get(0).getNombreMembres());
        assertEquals(2L, result.getStatsParEquipe().get(0).getDemandesPending());
    }

    @Test
    void getStats_SecondCall_UsesCache() {
        mockRepositoryCalls();

        dashboardService.getStats(); // First call
        DashboardStatsDto result = dashboardService.getStats(); // Second call (uses cache)

        assertNotNull(result);
        // Repository should only be called once (during first call)
        verify(userRepository, times(1)).countByRole(Role.EMPLOYEE);
    }

    @Test
    void getStats_NullAverageSolde_UsesZero() {
        when(userRepository.countByRole(Role.EMPLOYEE)).thenReturn(0L);
        when(userRepository.countByRole(Role.MANAGER)).thenReturn(0L);
        when(userRepository.countByRole(Role.HR)).thenReturn(0L);
        when(userRepository.averageSoldeConge()).thenReturn(null); // null average
        when(teamRepository.count()).thenReturn(0L);
        when(teamRepository.findAll()).thenReturn(List.of());
        when(leaveRepository.countByStatus(any())).thenReturn(0L);
        when(leaveRepository.countByStatusAndType(any(), any())).thenReturn(0L);

        DashboardStatsDto result = dashboardService.getStats();

        assertEquals(0.0, result.getSoldeMoyenConge());
    }

    // ──────────── refreshStats ────────────

    @Test
    void refreshStats_RecomputesStats() {
        mockRepositoryCalls();

        dashboardService.refreshStats();
        DashboardStatsDto result = dashboardService.getStats();

        assertNotNull(result);
        // After refreshStats, getStats returns cached value (no second compute)
        verify(userRepository, times(1)).countByRole(Role.EMPLOYEE);
    }

    @Test
    void refreshStats_AfterGetStats_Recomputes() {
        mockRepositoryCalls();

        dashboardService.getStats();    // First call
        dashboardService.refreshStats(); // Refresh

        // Now after refresh, getStats should use the refreshed cache
        DashboardStatsDto result = dashboardService.getStats();

        assertNotNull(result);
        // countByRole is called twice: first getStats + refreshStats
        verify(userRepository, times(2)).countByRole(Role.EMPLOYEE);
    }

    @Test
    void getStats_WithMultipleTeams() {
        Team team2 = new Team();
        team2.setId(2L);
        team2.setNom("QA Team");
        team2.setMembres(new ArrayList<>());

        when(userRepository.countByRole(any())).thenReturn(5L);
        when(userRepository.averageSoldeConge()).thenReturn(20.0);
        when(teamRepository.count()).thenReturn(2L);
        when(teamRepository.findAll()).thenReturn(List.of(team, team2));
        when(teamRepository.countMembersByTeamId(anyLong())).thenReturn(3L);
        when(leaveRepository.countByTeamIdAndStatus(anyLong(), any())).thenReturn(1L);
        when(leaveRepository.countByStatus(any())).thenReturn(5L);
        when(leaveRepository.countByStatusAndType(any(), any())).thenReturn(2L);

        DashboardStatsDto result = dashboardService.getStats();

        assertThat(result.getStatsParEquipe()).hasSize(2);
    }
}
