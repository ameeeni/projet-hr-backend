package tn.iteam.hrprojectbackend.leave.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.iteam.hrprojectbackend.users.entities.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Leave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy; // manager ou RH qui a validé/refusé

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    private LocalDate dateSoumission;

    private LocalDate dateValidation;

    private Integer nombreJours;

    private String motif;

    private String commentaireValidateur; // remarque du manager/RH

    @PrePersist
    public void prePersist() {
        this.dateSoumission = LocalDate.now();
        this.status = LeaveStatus.PENDING;
        this.nombreJours = (int) ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
    }
}
