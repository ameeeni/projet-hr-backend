package tn.iteam.hrprojectbackend.leave.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveValidationDto {
    @NotNull(message = "La décision est obligatoire")
    private LeaveStatus decision; // APPROVED ou REJECTED

    @Size(max = 500)
    private String commentaireValidateur;
}
