package tn.iteam.hrprojectbackend.users.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.users.entities.Role;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDto {
    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String poste;
    private String departement;
    private LocalDate dateEmbauche;
    private Integer soldeConge;
    private Role role;
}
