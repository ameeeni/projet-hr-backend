package tn.iteam.hrprojectbackend.users.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMapper teamMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks private TeamServiceImpl teamService;

    private Team team;
    private TeamRequest teamRequest;
    private TeamResponse teamResponse;
    private User employee;
    private User hrUser;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(1L);
        team.setNom("Dev Team");
        team.setDescription("Development team");
        team.setMembres(new ArrayList<>());

        teamRequest = new TeamRequest();
        teamRequest.setNom("Dev Team");
        teamRequest.setDescription("Development team");

        teamResponse = new TeamResponse();
        teamResponse.setId(1L);
        teamResponse.setNom("Dev Team");
        teamResponse.setDescription("Development team");

        employee = new User();
        employee.setId(10L);
        employee.setNom("Employee");
        employee.setRole(Role.EMPLOYEE);
        employee.setTeam(null);

        hrUser = new User();
        hrUser.setId(20L);
        hrUser.setNom("HR");
        hrUser.setRole(Role.HR);
    }

    // ──────────── createTeam ────────────

    @Test
    void createTeam_Success() {
        when(teamRepository.existsByNom("Dev Team")).thenReturn(false);
        when(teamMapper.toEntity(teamRequest)).thenReturn(team);
        when(teamRepository.save(team)).thenReturn(team);
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.createTeam(teamRequest);

        assertNotNull(result);
        assertEquals("Dev Team", result.getNom());
        verify(teamRepository).save(team);
    }

    @Test
    void createTeam_DuplicateName_ThrowsBusinessException() {
        when(teamRepository.existsByNom("Dev Team")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.createTeam(teamRequest));
        assertEquals("DUPLICATE_TEAM_NAME", ex.getCode());
        verify(teamRepository, never()).save(any());
    }

    // ──────────── getTeamById ────────────

    @Test
    void getTeamById_Success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.getTeamById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getTeamById_NotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeamById(99L));
    }

    // ──────────── getAllTeams ────────────

    @Test
    void getAllTeams_ReturnsList() {
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        List<TeamResponse> result = teamService.getAllTeams();

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllTeams_EmptyList() {
        when(teamRepository.findAll()).thenReturn(List.of());

        List<TeamResponse> result = teamService.getAllTeams();

        assertThat(result).isEmpty();
    }

    // ──────────── updateTeam ────────────

    @Test
    void updateTeam_Success() {
        teamRequest.setNom("New Name");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByNom("New Name")).thenReturn(false);
        when(teamRepository.save(team)).thenReturn(team);
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.updateTeam(1L, teamRequest);

        assertNotNull(result);
        verify(teamRepository).save(team);
    }

    @Test
    void updateTeam_SameName_NoDuplicateCheck() {
        // Same name → no duplicate check
        teamRequest.setNom("Dev Team");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.save(team)).thenReturn(team);
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.updateTeam(1L, teamRequest);

        assertNotNull(result);
        verify(teamRepository, never()).existsByNom(any());
    }

    @Test
    void updateTeam_DuplicateName_ThrowsBusinessException() {
        teamRequest.setNom("Other Team");
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.existsByNom("Other Team")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(1L, teamRequest));
        assertEquals("DUPLICATE_TEAM_NAME", ex.getCode());
    }

    @Test
    void updateTeam_NotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.updateTeam(99L, teamRequest));
    }

    // ──────────── deleteTeam ────────────

    @Test
    void deleteTeam_Success_WithMembers() {
        List<User> membres = new ArrayList<>();
        membres.add(employee);
        team.setMembres(membres);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertDoesNotThrow(() -> teamService.deleteTeam(1L));
        verify(userRepository).saveAll(membres);
        verify(teamRepository).delete(team);
        assertNull(employee.getTeam());
    }

    @Test
    void deleteTeam_Success_NoMembers() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));

        assertDoesNotThrow(() -> teamService.deleteTeam(1L));
        verify(teamRepository).delete(team);
    }

    @Test
    void deleteTeam_NotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> teamService.deleteTeam(99L));
        verify(teamRepository, never()).delete(any());
    }

    // ──────────── addMemberToTeam ────────────

    @Test
    void addMemberToTeam_Success() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.addMemberToTeam(1L, 10L);

        assertNotNull(result);
        assertEquals(team, employee.getTeam());
        verify(userRepository).save(employee);
    }

    @Test
    void addMemberToTeam_TeamNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.addMemberToTeam(99L, 10L));
    }

    @Test
    void addMemberToTeam_UserNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.addMemberToTeam(1L, 99L));
    }

    @Test
    void addMemberToTeam_HrRole_ThrowsBusinessException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(20L)).thenReturn(Optional.of(hrUser));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.addMemberToTeam(1L, 20L));
        assertEquals("HR_CANNOT_HAVE_TEAM", ex.getCode());
    }

    @Test
    void addMemberToTeam_AlreadyInTeam_ThrowsBusinessException() {
        employee.setTeam(team); // already in this team
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.addMemberToTeam(1L, 10L));
        assertEquals("USER_ALREADY_IN_TEAM", ex.getCode());
    }

    // ──────────── removeMemberFromTeam ────────────

    @Test
    void removeMemberFromTeam_Success() {
        employee.setTeam(team);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.removeMemberFromTeam(1L, 10L);

        assertNotNull(result);
        assertNull(employee.getTeam());
        verify(userRepository).save(employee);
    }

    @Test
    void removeMemberFromTeam_TeamNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.removeMemberFromTeam(99L, 10L));
    }

    @Test
    void removeMemberFromTeam_UserNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.removeMemberFromTeam(1L, 99L));
    }

    @Test
    void removeMemberFromTeam_UserNotInTeam_ThrowsBusinessException() {
        employee.setTeam(null); // not in any team
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.removeMemberFromTeam(1L, 10L));
        assertEquals("USER_NOT_IN_TEAM", ex.getCode());
    }

    @Test
    void removeMemberFromTeam_UserInDifferentTeam_ThrowsBusinessException() {
        Team otherTeam = new Team();
        otherTeam.setId(99L);
        employee.setTeam(otherTeam);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userRepository.findById(10L)).thenReturn(Optional.of(employee));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.removeMemberFromTeam(1L, 10L));
        assertEquals("USER_NOT_IN_TEAM", ex.getCode());
    }

    // ──────────── getMembersByTeam ────────────

    @Test
    void getMembersByTeam_Success() {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(10L);
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findByTeamId(1L)).thenReturn(List.of(employee));
        when(userMapper.toResponse(employee)).thenReturn(userResponse);

        List<UserResponse> result = teamService.getMembersByTeam(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getMembersByTeam_TeamNotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.getMembersByTeam(99L));
    }

    @Test
    void getMembersByTeam_EmptyList() {
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findByTeamId(1L)).thenReturn(List.of());

        List<UserResponse> result = teamService.getMembersByTeam(1L);

        assertThat(result).isEmpty();
    }

    // ──────────── getTeamByNom ────────────

    @Test
    void getTeamByNom_Success() {
        when(teamRepository.findByNom("Dev Team")).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        TeamResponse result = teamService.getTeamByNom("Dev Team");

        assertNotNull(result);
        assertEquals("Dev Team", result.getNom());
    }

    @Test
    void getTeamByNom_NotFound_ThrowsResourceNotFoundException() {
        when(teamRepository.findByNom("Unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.getTeamByNom("Unknown"));
    }

    // ──────────── getTeamsWithMembers ────────────

    @Test
    void getTeamsWithMembers_ReturnsList() {
        when(teamRepository.findTeamsWithMembers()).thenReturn(List.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        List<TeamResponse> result = teamService.getTeamsWithMembers();

        assertThat(result).hasSize(1);
    }

    @Test
    void getTeamsWithMembers_EmptyList() {
        when(teamRepository.findTeamsWithMembers()).thenReturn(List.of());

        List<TeamResponse> result = teamService.getTeamsWithMembers();

        assertThat(result).isEmpty();
    }

    // ──────────── getTeamByMemberId ────────────

    @Test
    void getTeamByMemberId_Found() {
        when(teamRepository.findByMemberId(10L)).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(teamResponse);

        Optional<TeamResponse> result = teamService.getTeamByMemberId(10L);

        assertTrue(result.isPresent());
    }

    @Test
    void getTeamByMemberId_NotFound() {
        when(teamRepository.findByMemberId(99L)).thenReturn(Optional.empty());

        Optional<TeamResponse> result = teamService.getTeamByMemberId(99L);

        assertFalse(result.isPresent());
    }
}
