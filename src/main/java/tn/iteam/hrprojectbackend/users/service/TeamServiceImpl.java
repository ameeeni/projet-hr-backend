package tn.iteam.hrprojectbackend.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iteam.hrprojectbackend.common.exception.BusinessException;
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.users.dto.TeamRequest;
import tn.iteam.hrprojectbackend.users.dto.TeamResponse;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.mapper.TeamMapper;
import tn.iteam.hrprojectbackend.users.mapper.UserMapper;
import tn.iteam.hrprojectbackend.users.repository.TeamRepository;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;
    private final UserMapper userMapper;

    @Override
    public TeamResponse createTeam(TeamRequest request) {
        if (teamRepository.existsByNom(request.getNom())) {
            throw new BusinessException("DUPLICATE_TEAM_NAME",
                    "Une équipe avec ce nom existe déjà");
        }

        Team team = teamMapper.toEntity(request);
        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", id));

        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Override
    public TeamResponse updateTeam(Long id, TeamRequest request) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", id));

        if (!team.getNom().equals(request.getNom())
                && teamRepository.existsByNom(request.getNom())) {
            throw new BusinessException("DUPLICATE_TEAM_NAME",
                    "Une équipe avec ce nom existe déjà");
        }

        team.setNom(request.getNom());
        team.setDescription(request.getDescription());

        return teamMapper.toResponse(teamRepository.save(team));
    }

    @Override
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", id));

        team.getMembres().forEach(user -> user.setTeam(null));
        userRepository.saveAll(team.getMembres());

        teamRepository.delete(team);
    }

    @Override
    public TeamResponse addMemberToTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        if (user.getRole() == Role.HR) {
            throw new BusinessException("HR_CANNOT_HAVE_TEAM",
                    "Un RH ne peut pas être affecté à une équipe");
        }

        if (user.getTeam() != null && user.getTeam().getId().equals(teamId)) {
            throw new BusinessException("USER_ALREADY_IN_TEAM",
                    "L'utilisateur est déjà dans cette équipe");
        }

        user.setTeam(team);
        userRepository.save(user);

        return teamMapper.toResponse(teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", teamId)));
    }

    @Override
    public TeamResponse removeMemberFromTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        if (user.getTeam() == null || !user.getTeam().getId().equals(teamId)) {
            throw new BusinessException("USER_NOT_IN_TEAM",
                    "L'utilisateur n'appartient pas à cette équipe");
        }

        user.setTeam(null);
        userRepository.save(user);

        return teamMapper.toResponse(teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", teamId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getMembersByTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Équipe", teamId);
        }

        return userRepository.findByTeamId(teamId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamByNom(String nom) {
        Team team = teamRepository.findByNom(nom)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", "nom", nom));

        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsWithMembers() {
        return teamRepository.findTeamsWithMembers()
                .stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamResponse> getTeamByMemberId(Long userId) {
        return teamRepository.findByMemberId(userId)
                .map(teamMapper::toResponse);
    }
}