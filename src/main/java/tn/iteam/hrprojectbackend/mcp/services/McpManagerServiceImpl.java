package tn.iteam.hrprojectbackend.mcp.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class McpManagerServiceImpl implements McpManagerService {

    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;

    @Override
    public List<Map<String, Object>> getPendingRequestsByTeam(Long teamId) {
        return leaveRepository.findPendingByTeamId(teamId)
                .stream()
                .map(req -> Map.<String, Object>of(
                        "employee", req.getEmployee().getNom() + " " + req.getEmployee().getPrenom(),
                        "type", req.getType().name(),
                        "status", req.getStatus().name()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTeamCalendar(Long teamId) {
        LocalDate today = LocalDate.now();

        long activeLeavesToday = leaveRepository
                .countActiveLeavesByTeamAndStatus(teamId, LeaveStatus.APPROVED, today);

        long teleworkingToday = leaveRepository
                .countActiveLeavesByTeamTypeAndStatus(teamId, LeaveType.TELETRAVAIL, LeaveStatus.APPROVED, today);

        return Map.of(
                "teamId", teamId,
                "activeLeavesToday", activeLeavesToday,
                "teleworkingToday", teleworkingToday
        );
    }

    @Override
    public Map<String, Object> getMyLeaveBalance(Long employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employé introuvable : " + employeeId));

        long teleworkUsed = leaveRepository.findByEmployeeIdAndType(employeeId, LeaveType.TELETRAVAIL)
                .stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .mapToLong(l -> l.getNombreJours() != null ? l.getNombreJours() : 0)
                .sum();

        long sickDaysUsed = leaveRepository.findByEmployeeIdAndType(employeeId, LeaveType.MALADIE)
                .stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .mapToLong(l -> l.getNombreJours() != null ? l.getNombreJours() : 0)
                .sum();

        return Map.of(
                "employeeId", employeeId,
                "nom", user.getNom() + " " + user.getPrenom(),
                "soldeCongeRestant", user.getSoldeConge() != null ? user.getSoldeConge() : 0,
                "joursMaladieUtilises", sickDaysUsed,
                "joursTeletravailUtilises", teleworkUsed
        );
    }

    @Override
    public List<Map<String, Object>> getMyLeaveHistory(Long employeeId) {
        return leaveRepository.findHistoryByEmployeeId(employeeId)
                .stream()
                .map(l -> Map.<String, Object>of(
                        "type", l.getType().name(),
                        "dateDebut", l.getDateDebut().toString(),
                        "dateFin", l.getDateFin().toString(),
                        "status", l.getStatus().name(),
                        "nombreJours", l.getNombreJours() != null ? l.getNombreJours() : 0,
                        "motif", l.getMotif() != null ? l.getMotif() : ""
                ))
                .collect(Collectors.toList());
    }
}