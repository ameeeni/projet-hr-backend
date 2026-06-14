package tn.iteam.hrprojectbackend.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iteam.hrprojectbackend.common.exception.BusinessException;
import tn.iteam.hrprojectbackend.common.exception.ResourceNotFoundException;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.events.EmployeeCreatedEvent;
import tn.iteam.hrprojectbackend.users.mapper.UserMapper;
import tn.iteam.hrprojectbackend.users.repository.TeamRepository;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final UserMapper userMapper;
    private final HrEventProducer hrEventProducer;


    @Override
    public UserResponse createUser(UserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL",
                    "Un utilisateur avec cet email existe déjà");
        }

        if (userRepository.existsByMatricule(request.getMatricule())) {
            throw new BusinessException("DUPLICATE_MATRICULE",
                    "Un utilisateur avec ce matricule existe déjà");
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Équipe", request.getTeamId()));
        }

        User manager = null;
        if (request.getManagerId() != null) {
            manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", request.getManagerId()));
        }

        if (request.getRole() == Role.HR && request.getTeamId() != null) {
            throw new BusinessException("HR_CANNOT_HAVE_TEAM",
                    "Un RH ne peut pas être affecté à une équipe");
        }

        User user = userMapper.toEntity(request);
        user.setTeam(team);
        user.setManager(manager);
        User saved = userRepository.save(user);

        hrEventProducer.sendEmployeeCreated(EmployeeCreatedEvent.builder()
                .employeeId(saved.getId())
                .nom(saved.getNom())
                .email(saved.getEmail())
                .poste(saved.getPoste())
                .role(saved.getRole())
                .build());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "username", username));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findByRole(role)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Équipe", teamId);
        }

        return userRepository.findByTeamId(teamId)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL",
                    "Un utilisateur avec cet email existe déjà");
        }

        if (!user.getMatricule().equals(request.getMatricule())
                && userRepository.existsByMatricule(request.getMatricule())) {
            throw new BusinessException("DUPLICATE_MATRICULE",
                    "Un utilisateur avec ce matricule existe déjà");
        }

        user.setMatricule(request.getMatricule());
        user.setNom(request.getNom());
        user.setEmail(request.getEmail());
        user.setPoste(request.getPoste());
        user.setDepartement(request.getDepartement());
        user.setDateEmbauche(request.getDateEmbauche());
        user.setRole(request.getRole());
        user.setSoldeConge(request.getSoldeConge());

        if (request.getRole() == Role.HR && request.getTeamId() != null) {
            throw new BusinessException("HR_CANNOT_HAVE_TEAM",
                    "Un RH ne peut pas être affecté à une équipe");
        }

        if (request.getTeamId() != null) {
            Team team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Équipe", request.getTeamId()));
            user.setTeam(team);
        } else {
            user.setTeam(null);
        }

        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager", request.getManagerId()));
            user.setManager(manager);
        } else {
            user.setManager(null);
        }

        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        userRepository.delete(user);
    }

    @Override
    public UserResponse assignToTeam(Long userId, Long teamId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipe", teamId));

        if (user.getRole() == Role.HR) {
            throw new BusinessException("HR_CANNOT_HAVE_TEAM",
                    "Un RH ne peut pas être affecté à une équipe");
        }

        if (user.getTeam() != null && user.getTeam().getId().equals(teamId)) {
            throw new BusinessException("USER_ALREADY_IN_TEAM",
                    "L'utilisateur est déjà dans cette équipe");
        }

        user.setTeam(team);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse assignManager(Long userId, Long managerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager", managerId));

        if (manager.getRole() != Role.MANAGER && manager.getRole() != Role.HR) {
            throw new BusinessException("INVALID_MANAGER_ROLE",
                    "Le responsable assigné doit avoir le rôle MANAGER ou HR");
        }

        if (user.getId().equals(manager.getId())) {
            throw new BusinessException("SELF_MANAGER_NOT_ALLOWED",
                    "Un utilisateur ne peut pas être son propre manager");
        }

        user.setManager(manager);
        return userMapper.toResponse(userRepository.save(user));
    }
}