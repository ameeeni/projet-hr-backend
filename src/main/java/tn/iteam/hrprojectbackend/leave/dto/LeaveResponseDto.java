package tn.iteam.hrprojectbackend.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveResponseDto {

    private Long id;
    private Long employeeId;
    private String employeeNom;
    private String employeeMatricule;
    private String teamNom;
    private LeaveType type;
    private LeaveStatus status;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateSoumission;
    private LocalDate dateValidation;
    private Integer nombreJours;
    private String motif;
    private String commentaireValidateur;
    private String validatedByNom;
}
