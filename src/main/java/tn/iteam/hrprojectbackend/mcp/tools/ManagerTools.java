package tn.iteam.hrprojectbackend.mcp.tools;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import tn.iteam.hrprojectbackend.common.security.SecurityUtils;
import tn.iteam.hrprojectbackend.mcp.services.McpManagerService;

import java.util.List;
import java.util.Map;

@Hidden
@Component
@RequiredArgsConstructor
public class ManagerTools {

    private final McpManagerService mcpManagerService;

    @Tool(name = "get_pending_requests_by_team", description = "Retourne les demandes en attente de l'équipe du manager connecté")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public List<Map<String, Object>> getPendingRequestsByTeam() {
        Long teamId = SecurityUtils.getCurrentUserTeamId();
        if (teamId == null) throw new IllegalStateException("Le manager connecté n'est pas associé à une équipe");
        return mcpManagerService.getPendingRequestsByTeam(teamId);
    }

    @Tool(name = "get_team_calendar", description = "Retourne le calendrier de l'équipe du manager connecté")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public Map<String, Object> getTeamCalendar() {
        Long teamId = SecurityUtils.getCurrentUserTeamId();
        if (teamId == null) throw new IllegalStateException("Le manager connecté n'est pas associé à une équipe");
        return mcpManagerService.getTeamCalendar(teamId);
    }
}
