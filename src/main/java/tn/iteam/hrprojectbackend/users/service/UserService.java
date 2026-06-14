package tn.iteam.hrprojectbackend.users.service;

import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.Role;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    UserResponse getUserByUsername(String username);


    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(Role role);

    List<UserResponse> getUsersByTeam(Long teamId);

    UserResponse updateUser(Long id, UserRequest request);

    void deleteUser(Long id);

    UserResponse assignToTeam(Long userId, Long teamId);

    UserResponse assignManager(Long userId, Long managerId);
}
