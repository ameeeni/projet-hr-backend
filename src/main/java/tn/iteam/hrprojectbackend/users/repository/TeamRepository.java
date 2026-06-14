package tn.iteam.hrprojectbackend.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iteam.hrprojectbackend.users.entities.Team;

import java.util.List;
import java.util.Optional;
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Chercher une équipe par nom
    Optional<Team> findByNom(String nom);

    // Vérifier si un nom d'équipe existe déjà
    boolean existsByNom(String nom);

    // Récupérer toutes les équipes qui ont au moins un membre
    @Query("SELECT DISTINCT t FROM Team t JOIN t.membres m")
    List<Team> findTeamsWithMembers();

    // Récupérer les équipes sans membres
    @Query("SELECT t FROM Team t WHERE t.membres IS EMPTY")
    List<Team> findEmptyTeams();

    // Chercher une équipe par l'id d'un de ses membres
    @Query("SELECT t FROM Team t JOIN t.membres m WHERE m.id = :userId")
    Optional<Team> findByMemberId(@Param("userId") Long userId);
    // Compter les membres d'une équipe
    @Query("SELECT COUNT(u) FROM User u WHERE u.team.id = :teamId")
    long countMembersByTeamId(@Param("teamId") Long teamId);
}
