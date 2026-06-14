package tn.iteam.hrprojectbackend.users.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;



@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)


    private Long id;

    private String matricule;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String poste;
    private String departement;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "solde_conge")
    private Integer soldeConge;

    @Enumerated(EnumType.STRING)
    private Role role; // EMPLOYEE, MANAGER, HR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id") // un employé peut etre une partie à une seule équipe
    private Team team; // null pour RH

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id") // un employé peut avoir un seul manager
    private User manager; // null pour HR et certains managers
}
