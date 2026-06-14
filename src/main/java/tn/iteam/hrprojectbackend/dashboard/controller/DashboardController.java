package tn.iteam.hrprojectbackend.dashboard.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iteam.hrprojectbackend.dashboard.dto.DashboardStatsDto;
import tn.iteam.hrprojectbackend.dashboard.services.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Statistiques RH globales")
public class DashboardController {
    private final DashboardService dashboardService;

    // Statistiques globales : MANAGER ou HR uniquement
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER')")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}

