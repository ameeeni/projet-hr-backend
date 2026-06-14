package tn.iteam.hrprojectbackend.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.iteam.hrprojectbackend.users.dto.UserRequest;
import tn.iteam.hrprojectbackend.users.dto.UserResponse;
import tn.iteam.hrprojectbackend.users.entities.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entity → Response
    @Mapping(source = "team.nom", target = "teamNom")
    @Mapping(source = "manager.nom", target = "managerNom")
    UserResponse toResponse(User user);

    // Request → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "manager", ignore = true)
    User toEntity(UserRequest request);

    // Liste
    List<UserResponse> toResponseList(List<User> users);
}
