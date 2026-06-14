package tn.iteam.hrprojectbackend.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByRole(Role role);

    List<User> findByTeamId(Long teamId);
    Optional<User> findByMatricule(String matricule);

    // Alias pour trouver par username (le matricule = username IAM)
    default Optional<User> findByUsername(String username) {
        return findByMatricule(username);
    }

    List<User> findByManagerId(Long managerId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMatricule(String matricule);
    // Compter par rôle
    long countByRole(Role role);

    // Solde congé moyen
    @Query("SELECT AVG(u.soldeConge) FROM User u WHERE u.role = 'EMPLOYEE'")
    Double averageSoldeConge();

    // Chercher un utilisateur par prénom (insensible à la casse)
    @Query("SELECT u FROM User u WHERE LOWER(u.prenom) = LOWER(:prenom)")
    Optional<User> findByPrenomIgnoreCase(String prenom);

    // Chercher des utilisateurs dont le prénom contient une partie du texte (insensible à la casse)
    @Query("SELECT u FROM User u WHERE LOWER(u.prenom) LIKE LOWER(CONCAT('%', :partialName, '%'))")
    List<User> findByPrenomContainingIgnoreCase(String partialName);
}
