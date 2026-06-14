package tn.iteam.hrprojectbackend.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import tn.iteam.hrprojectbackend.users.dto.TeamRequest;
import tn.iteam.hrprojectbackend.users.dto.TeamResponse;
import tn.iteam.hrprojectbackend.users.entities.Team;
import tn.iteam.hrprojectbackend.users.entities.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    // Entity → Response
    @Mapping(source = "membres", target = "nombreMembres",
            qualifiedByName = "countMembres")
    TeamResponse toResponse(Team team);

    // Request → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "membres", ignore = true)
    Team toEntity(TeamRequest request);

    // Liste
    List<TeamResponse> toResponseList(List<Team> teams);

    // Méthode custom pour compter les membres
    @Named("countMembres")
    default int countMembres(List<User> membres) {
        return membres != null ? membres.size() : 0;
    }
}
