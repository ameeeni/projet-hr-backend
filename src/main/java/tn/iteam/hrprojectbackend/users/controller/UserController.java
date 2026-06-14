package tn.iteam.hrprojectbackend.users.controller;

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
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.events.UserEventDto;
import tn.iteam.hrprojectbackend.users.mapper.UserMapper;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;
import tn.iteam.hrprojectbackend.users.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employés", description = "Gestion des employés")
public class UserController {

    private final UserService userService;
    private final HrEventProducer hrEventProducer;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    // Créer un employé : HR uniquement
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // TEST : Simuler un événement IAM user-created
    @PostMapping("/test-kafka")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<String> testKafka(@Valid @RequestBody UserEventDto userEvent) {
        hrEventProducer.sendUserCreated(userEvent);
        return ResponseEntity.ok("Événement user-created envoyé vers Kafka");
    }

    // Obtenir les informations de l'utilisateur connecté
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName(); // Username IAM (ex: ameni.bramli ou kawther.m)

        log.info("🔍 Récupération des infos pour l'utilisateur connecté: {}", username);
        log.info("🔐 Authorities: {}", auth.getAuthorities());

        // Extraire le prénom du username IAM (partie avant le point)
        String[] parts = username.split("\\.");
        if (parts.length == 0) {
            log.error("❌ Format de username IAM invalide: {}", username);
            throw new ResourceNotFoundException("Utilisateur", "username IAM", username);
        }

        String prenom = parts[0]; // Ex: "ameni" depuis "ameni.bramli" ou "kawther" depuis "kawther.m"
        log.info("🔍 Extraction du prénom: {}", prenom);

        // Chercher l'utilisateur par prénom (insensible à la casse)
        User user = userRepository.findByPrenomIgnoreCase(prenom)
                .orElseGet(() -> {
                    log.warn("⚠️ Recherche exacte échouée, recherche partielle pour: {}", prenom);
                    // Si pas trouvé par prénom exact, chercher par partie du prénom
                    List<User> users = userRepository.findByPrenomContainingIgnoreCase(prenom);
                    if (users.isEmpty()) {
                        log.error("❌ Aucun utilisateur trouvé avec prénom contenant: {}", prenom);
                        throw new ResourceNotFoundException("Utilisateur", "prénom extrait de username IAM", prenom);
                    }
                    log.info("✅ Trouvé {} utilisateur(s) correspondant", users.size());
                    return users.get(0); // Prendre le premier résultat
                });

        log.info("✅ Utilisateur trouvé: id={}, nom={} {}, email={}",
                user.getId(), user.getPrenom(), user.getNom(), user.getEmail());

        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    // Voir un employé : MANAGER ou HR
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    // Lister tous les employés : MANAGER ou HR
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Filtrer par rôle : HR uniquement
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    // Membres d'une équipe : MANAGER ou HR
    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<UserResponse>> getByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(userService.getUsersByTeam(teamId));
    }

    // Modifier un employé : HR uniquement
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Supprimer un employé : HR uniquement
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Affecter à une équipe : HR uniquement
    @PatchMapping("/{userId}/team/{teamId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<UserResponse> assignToTeam(
            @PathVariable Long userId,
            @PathVariable Long teamId) {
        return ResponseEntity.ok(userService.assignToTeam(userId, teamId));
    }

    // Affecter un manager : HR uniquement
    @PatchMapping("/{userId}/manager/{managerId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<UserResponse> assignManager(
            @PathVariable Long userId,
            @PathVariable Long managerId) {
        return ResponseEntity.ok(userService.assignManager(userId, managerId));
    }
}

