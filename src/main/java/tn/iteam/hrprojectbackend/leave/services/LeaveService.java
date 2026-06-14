package tn.iteam.hrprojectbackend.leave.services;

import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.dto.LeaveValidationDto;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;

import java.util.List;

public interface LeaveService {

    // Employé soumet une demande
    LeaveResponseDto submitRequest(Long employeeId, LeaveRequest dto);

    // Employé annule sa demande
    LeaveResponseDto cancelRequest(Long requestId, Long employeeId);

    // Manager ou RH valide/refuse
    LeaveResponseDto validateRequest(Long requestId, Long validatorId,
                                     LeaveValidationDto dto);

    // Récupérer une demande par id
    LeaveResponseDto getById(Long id);

    // Historique complet d'un employé
    List<LeaveResponseDto> getHistoryByEmployee(Long employeeId);

    // Demandes en attente pour une équipe (manager)
    List<LeaveResponseDto> getPendingByTeam(Long teamId);

    // Toutes les demandes (RH)
    List<LeaveResponseDto> getAllRequests();

    // Demandes par statut (RH)
    List<LeaveResponseDto> getByStatus(LeaveStatus status);

    // Demandes d'un employé par statut (pour filtrage)
    List<LeaveResponseDto> getByEmployeeAndStatus(Long employeeId, LeaveStatus status);

    // Vérifier si un username correspond à un employeeId
    boolean isOwner(String username, Long employeeId);
}
