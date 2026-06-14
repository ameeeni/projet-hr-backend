package tn.iteam.hrprojectbackend.users.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.iteam.hrprojectbackend.users.dto.TeamRequest;
import tn.iteam.hrprojectbackend.users.dto.TeamResponse;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.service.TeamService;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Équipes", description = "Gestion des équipes")
public class TeamController {

    private final TeamService teamService;

    // Créer une équipe : HR uniquement
    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<TeamResponse> createTeam(
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(teamService.createTeam(request));
    }

    // Voir une équipe : MANAGER ou HR
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<TeamResponse> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    // Lister toutes les équipes : MANAGER ou HR
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<TeamResponse>> getAll() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    // Équipes avec membres : MANAGER ou HR
    @GetMapping("/with-members")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<TeamResponse>> getWithMembers() {
        return ResponseEntity.ok(teamService.getTeamsWithMembers());
    }

    // Modifier une équipe : HR uniquement
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<TeamResponse> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.updateTeam(id, request));
    }

    // Supprimer une équipe : HR uniquement
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    // Ajouter un membre : HR uniquement
    @PostMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<TeamResponse> addMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(teamService.addMemberToTeam(teamId, userId));
    }

    // Retirer un membre : HR uniquement
    @DeleteMapping("/{teamId}/members/{userId}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<TeamResponse> removeMember(
            @PathVariable Long teamId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(teamService.removeMemberFromTeam(teamId, userId));
    }

    // Voir les membres d'une équipe : MANAGER ou HR
    @GetMapping("/{teamId}/members")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<List<UserResponse>> getMembers(
            @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getMembersByTeam(teamId));
    }

    // Chercher par nom : MANAGER ou HR
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('MANAGER', 'HR')")
    public ResponseEntity<TeamResponse> getByNom(
            @RequestParam String nom) {
        return ResponseEntity.ok(teamService.getTeamByNom(nom));
    }
}
