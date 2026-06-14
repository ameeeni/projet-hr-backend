package tn.iteam.hrprojectbackend.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRequest {
    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;
    @Size(max = 255, message = "La description ne peut pas dépasser 255 caractères")
    private String description;
}
