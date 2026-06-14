package tn.iteam.hrprojectbackend.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.users.entities.Role;

import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String poste;
    private String departement;
    private LocalDate dateEmbauche;
    private Integer soldeConge;
    private Role role;
    private String teamNom;
    private String managerNom;
}
