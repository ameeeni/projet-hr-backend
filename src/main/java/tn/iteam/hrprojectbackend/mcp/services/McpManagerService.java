package tn.iteam.hrprojectbackend.mcp.services;

import java.util.List;
import java.util.Map;

public interface McpManagerService {
    List<Map<String, Object>> getPendingRequestsByTeam(Long teamId);
    Map<String, Object> getTeamCalendar(Long teamId);
    Map<String, Object> getMyLeaveBalance(Long employeeId);
    List<Map<String, Object>> getMyLeaveHistory(Long employeeId);
}
