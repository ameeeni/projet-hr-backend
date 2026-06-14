package tn.iteam.hrprojectbackend.leave.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tn.iteam.hrprojectbackend.leave.dto.LeaveRequest;
import tn.iteam.hrprojectbackend.leave.dto.LeaveResponseDto;
import tn.iteam.hrprojectbackend.leave.entities.Leave;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LeaveMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(expression = "java(leave.getEmployee().getNom() + \" \" + leave.getEmployee().getPrenom())", target = "employeeNom")
    @Mapping(source = "employee.matricule", target = "employeeMatricule")
    @Mapping(source = "employee.team.nom", target = "teamNom")
    @Mapping(source = "validatedBy.nom", target = "validatedByNom")
    LeaveResponseDto toResponse(Leave leave);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "validatedBy", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateSoumission", ignore = true)
    @Mapping(target = "dateValidation", ignore = true)
    @Mapping(target = "nombreJours", ignore = true)
    @Mapping(target = "commentaireValidateur", ignore = true)
    Leave toEntity(LeaveRequest dto);

    List<LeaveResponseDto> toResponseList(List<Leave> leave);
}