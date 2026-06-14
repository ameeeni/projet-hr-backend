package tn.iteam.hrprojectbackend.users.service;

import tn.iteam.hrprojectbackend.users.dto.TeamRequest;
import tn.iteam.hrprojectbackend.users.dto.TeamResponse;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;

import java.util.List;
import java.util.Optional;

public interface TeamService {

    // CRUD de base
    TeamResponse createTeam(TeamRequest request);

    TeamResponse getTeamById(Long id);

    List<TeamResponse> getAllTeams();

    TeamResponse updateTeam(Long id, TeamRequest request);

    void deleteTeam(Long id);

    // Gestion des membres
    TeamResponse addMemberToTeam(Long teamId, Long userId);

    TeamResponse removeMemberFromTeam(Long teamId, Long userId);

    List<UserResponse> getMembersByTeam(Long teamId);

    // Recherche
    TeamResponse getTeamByNom(String nom);

    List<TeamResponse> getTeamsWithMembers();

    Optional<TeamResponse> getTeamByMemberId(Long userId);
}
