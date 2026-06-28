package tn.iteam.hrprojectbackend.dashboard.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iteam.hrprojectbackend.dashboard.dto.DashboardStatsDto;
import tn.iteam.hrprojectbackend.dashboard.dto.TeamStatsDto;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.repositories.LeaveRepository;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.repository.TeamRepository;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final LeaveRepository leaveRepository;

    private final AtomicReference<DashboardStatsDto> cachedStats = new AtomicReference<>();

    public DashboardStatsDto getStats() {
        DashboardStatsDto stats = cachedStats.get();
        if (stats == null) {
            log.info(">>> Dashboard : calcul initial des stats");
            DashboardStatsDto computed = computeStats();
            cachedStats.compareAndSet(null, computed);
            stats = cachedStats.get();
        }
        return stats;
    }

    public void refreshStats() {
        log.info(">>> Dashboard : rafraîchissement via événement Kafka");
        cachedStats.set(computeStats());
    }

    private DashboardStatsDto computeStats() {

        long totalEmployes  = userRepository.countByRole(Role.EMPLOYEE);
        long totalManagers  = userRepository.countByRole(Role.MANAGER);
        long totalHR        = userRepository.countByRole(Role.HR);
        double soldeMoyen   = userRepository.averageSoldeConge() != null
                ? userRepository.averageSoldeConge() : 0.0;

        long totalEquipes = teamRepository.count();

        List<TeamStatsDto> statsParEquipe = teamRepository.findAll()
                .stream()
                .map(team -> TeamStatsDto.builder()
                        .teamNom(team.getNom())
                        .nombreMembres(teamRepository.countMembersByTeamId(team.getId()))
                        .demandesPending(leaveRepository.countByTeamIdAndStatus(
                                team.getId(), LeaveStatus.PENDING))
                        .build())
                .toList();

        long demandesPending    = leaveRepository.countByStatus(LeaveStatus.PENDING);
        long demandesApprouves  = leaveRepository.countByStatus(LeaveStatus.APPROVED);
        long demandesRefuses    = leaveRepository.countByStatus(LeaveStatus.REJECTED);
        long demandesAnnules    = leaveRepository.countByStatus(LeaveStatus.CANCELLED);

        long congesPending      = leaveRepository.countByStatusAndType(
                LeaveStatus.PENDING, LeaveType.CONGE_ANNUEL);
        long maladiePending     = leaveRepository.countByStatusAndType(
                LeaveStatus.PENDING, LeaveType.MALADIE);
        long teletravailPending = leaveRepository.countByStatusAndType(
                LeaveStatus.PENDING, LeaveType.TELETRAVAIL);

        return DashboardStatsDto.builder()
                .totalEmployes(totalEmployes)
                .totalManagers(totalManagers)
                .totalHR(totalHR)
                .totalEquipes(totalEquipes)
                .statsParEquipe(statsParEquipe)
                .demandesPending(demandesPending)
                .demandesApprouveesTotal(demandesApprouves)
                .demandesRefuseesTotal(demandesRefuses)
                .demandesAnnuleesTotal(demandesAnnules)
                .congesPending(congesPending)
                .maladiePending(maladiePending)
                .teletravailPending(teletravailPending)
                .soldeMoyenConge(soldeMoyen)
                .build();
    }
}
