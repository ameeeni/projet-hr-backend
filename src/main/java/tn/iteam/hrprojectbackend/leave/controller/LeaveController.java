package tn.iteam.hrprojectbackend.leave.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.dto.LeaveValidationDto;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.services.LeaveService;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Congés", description = "Gestion des demandes de congé")
public class LeaveController {

    private final LeaveService leaveService;
    private final UserRepository userRepository;


    // Employé soumet une demande (tous les rôles)
    @PostMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<LeaveResponseDto> submit(
            @PathVariable Long employeeId,
            @Valid @RequestBody LeaveRequest dto) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Username du JWT IAM

        // Vérifier si c'est un employé qui soumet pour lui-même
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        if (isEmployee) {
            // Chercher l'utilisateur par matricule (qui correspond au username IAM)
            User user = userRepository.findByMatricule(username)
                    .orElseThrow(() -> new tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException(
                            "Utilisateur", "matricule", username));

            // Vérifier qu'il soumet pour lui-même
            if (!user.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(leaveService.submitRequest(employeeId, dto));
    }

    // Employé annule sa demande (EMPLOYEE ou MANAGER seulement)
    @PatchMapping("/{requestId}/cancel/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER')")
    public ResponseEntity<LeaveResponseDto> cancel(
            @PathVariable Long requestId,
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.cancelRequest(requestId, employeeId));
    }

    // Manager ou RH valide/refuse
    @PatchMapping("/{requestId}/validate/validator/{validatorId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<LeaveResponseDto> validate(
            @PathVariable Long requestId,
            @PathVariable Long validatorId,
            @Valid @RequestBody LeaveValidationDto dto) {
        return ResponseEntity.ok(leaveService.validateRequest(requestId, validatorId, dto));
    }

    // Voir une demande (tous les rôles authentifiés)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<LeaveResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leaveService.getById(id));
    }

    // Historique d'un employé (tous les rôles)
    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getHistory(
            @PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getHistoryByEmployee(employeeId));
    }

    // Demandes en attente pour une équipe (MANAGER ou HR)
    @GetMapping("/team/{teamId}/pending")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getPendingByTeam(
            @PathVariable Long teamId) {
        return ResponseEntity.ok(leaveService.getPendingByTeam(teamId));
    }

    // Toutes les demandes (HR uniquement)
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getAll() {
        return ResponseEntity.ok(leaveService.getAllRequests());
    }

    // Par statut (HR uniquement)
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<LeaveResponseDto>> getByStatus(
            @PathVariable LeaveStatus status) {
        return ResponseEntity.ok(leaveService.getByStatus(status));
    }

    // Obtenir les demandes de l'utilisateur connecté (auto-détection via JWT)
    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getMyRequests(
            @RequestParam(required = false) LeaveStatus status) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Username IAM (ex: ameni.bramli)

        log.info("🔍 Récupération des demandes pour l'utilisateur IAM: {}", username);

        // Extraire le prénom du username IAM
        String[] parts = username.split("\\.");
        if (parts.length == 0) {
            throw new tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException(
                    "Utilisateur", "username IAM", username);
        }

        String prenom = parts[0];
        log.info("🔍 Extraction du prénom: {}", prenom);

        // Chercher l'utilisateur par prénom
        User user = userRepository.findByPrenomIgnoreCase(prenom)
                .orElseGet(() -> {
                    List<User> users = userRepository.findByPrenomContainingIgnoreCase(prenom);
                    if (users.isEmpty()) {
                        log.error("❌ Utilisateur non trouvé avec prénom: {}", prenom);
                        throw new tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException(
                                "Utilisateur", "prénom extrait de username IAM", prenom);
                    }
                    return users.get(0);
                });

        List<LeaveResponseDto> result = (status == null)
                ? leaveService.getHistoryByEmployee(user.getId())
                : leaveService.getByEmployeeAndStatus(user.getId(), status);

        log.info("✅ Trouvé {} demande(s) pour l'utilisateur {}", result.size(), username);

        return ResponseEntity.ok(result);
    }

    // Nouveau endpoint : obtenir les demandes d'un employé avec filtrage optionnel par statut
    // Accessible par l'employé lui-même, son manager ou RH
    @GetMapping("/employee/{employeeId}/requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<List<LeaveResponseDto>> getEmployeeRequests(
            @PathVariable Long employeeId,
            @RequestParam(required = false) LeaveStatus status) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Username du JWT IAM

        // Vérifier si l'utilisateur a le rôle EMPLOYEE
        boolean isEmployee = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));

        // Si c'est un employé, vérifier qu'il consulte bien ses propres demandes
        if (isEmployee) {
            // Chercher l'utilisateur par matricule (qui correspond au username IAM)
            User user = userRepository.findByMatricule(username)
                    .orElseThrow(() -> new tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException(
                            "Utilisateur", "matricule", username));

            // Vérifier qu'il consulte ses propres demandes
            if (!user.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        // Récupérer les demandes avec ou sans filtre de statut
        List<LeaveResponseDto> result = (status == null)
                ? leaveService.getHistoryByEmployee(employeeId)
                : leaveService.getByEmployeeAndStatus(employeeId, status);

        return ResponseEntity.ok(result);
    }
}

