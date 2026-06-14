package tn.iteam.hrprojectbackend.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {

    // ── Employés ──────────────────────────────
    private long totalEmployes;
    private long totalManagers;
    private long totalHR;

    // ── Équipes ───────────────────────────────
    private long totalEquipes;
    private List<TeamStatsDto> statsParEquipe;

    // ── Demandes ──────────────────────────────
    private long demandesPending;
    private long demandesApprouveesTotal;
    private long demandesRefuseesTotal;
    private long demandesAnnuleesTotal;

    // ── Congés ────────────────────────────────
    private long congesPending;
    private long maladiePending;
    private long teletravailPending;

    // ── Solde congé ───────────────────────────
    private double soldeMoyenConge;
}