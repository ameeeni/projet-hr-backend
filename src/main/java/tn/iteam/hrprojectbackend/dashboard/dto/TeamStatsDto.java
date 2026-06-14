package tn.iteam.hrprojectbackend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamStatsDto {
    private String teamNom;
    private long nombreMembres;
    private long demandesPending;
}
