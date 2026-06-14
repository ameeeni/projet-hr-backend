package tn.iteam.hrprojectbackend.leave.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iteam.hrprojectbackend.leave.entities.Leave;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    // Toutes les demandes d'un employé
    List<Leave> findByEmployeeId(Long employeeId);

    // Demandes par statut
    List<Leave> findByStatus(LeaveStatus status);

    // Demandes d'un employé par statut
    List<Leave> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);

    // Demandes d'un employé par type
    List<Leave> findByEmployeeIdAndType(Long employeeId, LeaveType type);

    // Demandes en attente pour une équipe (pour le manager)
    @Query("""
        SELECT lr FROM Leave lr
        WHERE lr.employee.team.id = :teamId
        AND lr.status = tn.iteam.hrprojectbackend.leave.entities.LeaveStatus.PENDING
    """)
    List<Leave> findPendingByTeamId(@Param("teamId") Long teamId);


    // Historique des congés d'un employé
    @Query("""
        SELECT lr FROM Leave lr
        WHERE lr.employee.id = :employeeId
        ORDER BY lr.dateSoumission DESC
    """)
    List<Leave> findHistoryByEmployeeId(@Param("employeeId") Long employeeId);
    // Compter par statut
    long countByStatus(LeaveStatus status);

    // Compter par statut ET type
    long countByStatusAndType(LeaveStatus status, LeaveType type);

    // Compter par statut pour une équipe
    @Query("""
    SELECT COUNT(l) FROM Leave l
    WHERE l.employee.team.id = :teamId
    AND l.status = :status
""")
    long countByTeamIdAndStatus(@Param("teamId") Long teamId,
                                @Param("status") LeaveStatus status);
    // Fix: remplacer les strings par les enums dans findOverlappingRequests
    @Query("""
    SELECT lr FROM Leave lr
    WHERE lr.employee.id = :employeeId
    AND lr.status NOT IN (tn.iteam.hrprojectbackend.leave.entities.LeaveStatus.REJECTED, tn.iteam.hrprojectbackend.leave.entities.LeaveStatus.CANCELLED)
    AND lr.dateDebut <= :dateFin
    AND lr.dateFin >= :dateDebut
""")
    List<Leave> findOverlappingRequests(
            @Param("employeeId") Long employeeId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    // Compter les congés actifs aujourd'hui par équipe et statut
    @Query("""
    SELECT COUNT(l) FROM Leave l
    WHERE l.employee.team.id = :teamId
    AND l.status = :status
    AND l.dateDebut <= :today
    AND l.dateFin >= :today
""")
    long countActiveLeavesByTeamAndStatus(
            @Param("teamId") Long teamId,
            @Param("status") LeaveStatus status,
            @Param("today") LocalDate today);

    // Compter les congés actifs aujourd'hui par équipe, type et statut
    @Query("""
    SELECT COUNT(l) FROM Leave l
    WHERE l.employee.team.id = :teamId
    AND l.type = :type
    AND l.status = :status
    AND l.dateDebut <= :today
    AND l.dateFin >= :today
""")
    long countActiveLeavesByTeamTypeAndStatus(
            @Param("teamId") Long teamId,
            @Param("type") LeaveType type,
            @Param("status") LeaveStatus status,
            @Param("today") LocalDate today);

}
