package tn.iteam.hrprojectbackend.leave.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iteam.hrprojectbackend.common.exception.BusinessException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.dto.LeaveValidationDto;
import tn.iteam.hrprojectbackend.leave.entities.Leave;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.events.LeaveSubmittedEvent;
import tn.iteam.hrprojectbackend.leave.events.LeaveValidatedEvent;
import tn.iteam.hrprojectbackend.leave.mapper.LeaveMapper;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static tn.iteam.hrprojectbackend.common.utils.ErrorConstant.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final LeaveMapper leaveMapper;
    private final HrEventProducer hrEventProducer;


    @Override
    public LeaveResponseDto submitRequest(Long employeeId, LeaveRequest dto) {

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() ->  new ResourceNotFoundException(
                        "Employé introuvable avec l'ID: " + employeeId +
                        ". Veuillez vérifier que cet employé existe dans la base de données.",
                        employeeId));

        // Règle 1 : date fin >= date début
        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new BusinessException(INVALID_DATE_RANGE, "La date fin doit etre avant la date de debut");
        }

        // Règle 2 : pas de chevauchement
        List<Leave> overlapping = leaveRequestRepository.findOverlappingRequests(
                employeeId, dto.getDateDebut(), dto.getDateFin());
        if (!overlapping.isEmpty()) {
            throw new BusinessException(OVERLAPPING_REQUEST,"Une demande existe déjà sur cette période");
        }

        // Règle 3 : solde suffisant pour congé annuel
        if (dto.getType() == LeaveType.CONGE_ANNUEL) {
            int jours = (int) ChronoUnit.DAYS.between(dto.getDateDebut(), dto.getDateFin()) + 1;
            if (employee.getSoldeConge() == null || employee.getSoldeConge() < jours) {
                throw new BusinessException(INSUFFICIENT_BALANCE,"Solde de congé insuffisant");
            }
        }

        Leave leaveRequest = leaveMapper.toEntity(dto);
        leaveRequest.setEmployee(employee);

        Leave saved = leaveRequestRepository.save(leaveRequest);

        hrEventProducer.sendLeaveSubmitted(LeaveSubmittedEvent.builder()
                .leaveRequestId(saved.getId())
                .employeeId(employee.getId())
                .employeeNom(employee.getNom())
                .employeeEmail(employee.getEmail())
                .type(saved.getType())
                .dateDebut(saved.getDateDebut())
                .dateFin(saved.getDateFin())
                .nombreJours(saved.getNombreJours())
                .motif(saved.getMotif())
                .build());


        return leaveMapper.toResponse(saved);
    }

    @Override
    public LeaveResponseDto cancelRequest(Long requestId, Long employeeId) {

        Leave leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(REQUEST_NOT_FOUND,"Demande introuvable"));

        // Seul l'employé concerné peut annuler
        if (!leaveRequest.getEmployee().getId().equals(employeeId)) {
            throw new BusinessException(CANNOT_CANCEL_REQUEST,"Vous ne pouvez pas annuler cette demande");
        }

        // On peut annuler seulement si PENDING
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(REQUEST_ALREADY_PROCESSED,"Seules les demandes en attente peuvent être annulées");
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        return leaveMapper.toResponse(leaveRequestRepository.save(leaveRequest));
    }

    @Override
    public LeaveResponseDto validateRequest(Long requestId, Long validatorId,
                                            LeaveValidationDto dto) {

        Leave leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(REQUEST_NOT_FOUND,"Demande introuvable"));

        User validator = userRepository.findById(validatorId)
                .orElseThrow(() -> new BusinessException(VALIDATOR_NOT_FOUND,"Validateur introuvable"));

        // Seul MANAGER ou RH peut valider
        if (validator.getRole() != Role.MANAGER && validator.getRole() != Role.HR) {
            throw new BusinessException(NOT_VALID_VALIDATOR,"Vous n'avez pas les droits pour valider");
        }

        // Seulement si encore PENDING
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException(TREATED_REQUEST,"Cette demande a déjà été traitée");
        }

        leaveRequest.setStatus(dto.getDecision());
        leaveRequest.setValidatedBy(validator);
        leaveRequest.setDateValidation(LocalDate.now());
        leaveRequest.setCommentaireValidateur(dto.getCommentaireValidateur());

        // Si approuvé → déduire le solde pour congé annuel
        if (dto.getDecision() == LeaveStatus.APPROVED
                && leaveRequest.getType() == LeaveType.CONGE_ANNUEL) {
            User employee = leaveRequest.getEmployee();
            employee.setSoldeConge(employee.getSoldeConge() - leaveRequest.getNombreJours());
            userRepository.save(employee);
        }

        Leave saved = leaveRequestRepository.save(leaveRequest);

        // Publier l'événement Kafka
        hrEventProducer.sendLeaveValidated(LeaveValidatedEvent.builder()
                .leaveRequestId(saved.getId())
                .employeeId(saved.getEmployee().getId())
                .employeeEmail(saved.getEmployee().getEmail())
                .decision(saved.getStatus())
                .commentaire(saved.getCommentaireValidateur())
                .validatedByNom(validator.getNom())
                .build());

        return leaveMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveResponseDto getById(Long id) {
        return leaveMapper.toResponse(
                leaveRequestRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Demande de congé", id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getHistoryByEmployee(Long employeeId) {
        return leaveMapper.toResponseList(
                leaveRequestRepository.findHistoryByEmployeeId(employeeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getPendingByTeam(Long teamId) {
        return leaveMapper.toResponseList(
                leaveRequestRepository.findPendingByTeamId(teamId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getAllRequests() {
        return leaveMapper.toResponseList(leaveRequestRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getByStatus(LeaveStatus status) {
        return leaveMapper.toResponseList(
                leaveRequestRepository.findByStatus(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveResponseDto> getByEmployeeAndStatus(Long employeeId, LeaveStatus status) {
        return leaveMapper.toResponseList(
                leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(String username, Long employeeId) {
        // Récupérer l'utilisateur connecté par son email (username dans JWT)
        Optional<User> authenticatedUser = userRepository.findByEmail(username);

        if (authenticatedUser.isEmpty()) {
            return false;
        }

        // Vérifier que l'ID de l'utilisateur connecté correspond à l'employeeId demandé
        return authenticatedUser.get().getId().equals(employeeId);
    }
}
