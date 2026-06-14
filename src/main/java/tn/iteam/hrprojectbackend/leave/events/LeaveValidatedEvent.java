package tn.iteam.hrprojectbackend.leave.events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveValidatedEvent {
    private Long leaveRequestId;
    private Long employeeId;
    private String employeeEmail;
    private LeaveStatus decision;       // APPROVED ou REJECTED
    private String commentaire;
    private String validatedByNom;
}
