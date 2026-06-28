package tn.iteam.hrprojectbackend.dashboard.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.iteam.hrprojectbackend.dashboard.dto.DashboardStatsDto;
import tn.iteam.hrprojectbackend.dashboard.services.DashboardService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private DashboardService dashboardService;

    @InjectMocks private DashboardController dashboardController;

    @Test
    void getStats_Returns200() {
        DashboardStatsDto dto = DashboardStatsDto.builder()
                .totalEmployes(10L)
                .totalManagers(2L)
                .totalHR(1L)
                .totalEquipes(3L)
                .statsParEquipe(List.of())
                .demandesPending(5L)
                .demandesApprouveesTotal(8L)
                .build();

        when(dashboardService.getStats()).thenReturn(dto);

        ResponseEntity<DashboardStatsDto> result = dashboardController.getStats();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(10L, result.getBody().getTotalEmployes());
    }
}
