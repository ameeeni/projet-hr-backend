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
public class EmployeeTools {

    private final McpManagerService mcpManagerService;

    @Tool(name = "get_my_leave_balance", description = "Retourne le solde de congés de l'employé connecté")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public Map<String, Object> getMyLeaveBalance() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return mcpManagerService.getMyLeaveBalance(employeeId);
    }

    @Tool(name = "get_my_leave_history", description = "Retourne l'historique de congés de l'employé connecté")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public List<Map<String, Object>> getMyLeaveHistory() {
        Long employeeId = SecurityUtils.getCurrentUserId();
        return mcpManagerService.getMyLeaveHistory(employeeId);
    }
}
