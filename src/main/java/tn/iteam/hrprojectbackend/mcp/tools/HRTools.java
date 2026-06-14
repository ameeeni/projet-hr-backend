package tn.iteam.hrprojectbackend.mcp.tools;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import tn.iteam.hrprojectbackend.dashboard.dto.DashboardStatsDto;
import tn.iteam.hrprojectbackend.dashboard.services.DashboardService;

import java.util.Map;

@Hidden
@Component
@RequiredArgsConstructor
public class HRTools {

    private final DashboardService dashboardService;

    // Statistiques globales : HR uniquement
    @Tool(name = "get_dashboard_stats", description = "Retourne les statistiques globales RH")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public Map<String, Object> getDashboardStats() {
        DashboardStatsDto stats = dashboardService.getStats();
        return Map.of(
                "totalEmployees", stats.getTotalEmployes(),
                "totalManagers", stats.getTotalManagers(),
                "totalHR", stats.getTotalHR(),
                "totalEquipes", stats.getTotalEquipes(),
                "demandesPending", stats.getDemandesPending(),
                "demandesApprouvees", stats.getDemandesApprouveesTotal(),
                "demandesRefusees", stats.getDemandesRefuseesTotal(),
                "demandesAnnulees", stats.getDemandesAnnuleesTotal(),
                "soldeMoyenConge", stats.getSoldeMoyenConge()
        );
    }

    // Demandes en attente par type : HR uniquement
    @Tool(name = "get_pending_by_type", description = "Retourne le nombre de demandes en attente par type")
    @PreAuthorize("hasRole('HR')")
    public Map<String, Object> getPendingByType() {
        DashboardStatsDto stats = dashboardService.getStats();
        return Map.of(
                "congesPending", stats.getCongesPending(),
                "maladiePending", stats.getMaladiePending(),
                "teletravailPending", stats.getTeletravailPending()
        );
    }
}
