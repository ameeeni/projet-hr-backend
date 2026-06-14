package tn.iteam.hrprojectbackend.leave.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveSubmittedEvent {
    private Long leaveRequestId;
    private Long employeeId;
    private String employeeNom;
    private String employeeEmail;
    private LeaveType type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Integer nombreJours;
    private String motif;
}
