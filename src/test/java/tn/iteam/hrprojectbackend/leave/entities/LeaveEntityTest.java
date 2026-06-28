package tn.iteam.hrprojectbackend.leave.entities;

import org.junit.jupiter.api.Test;
import tn.iteam.hrprojectbackend.users.entities.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LeaveEntityTest {

    @Test
    void prePersist_SetsStatusAndDates() {
        Leave leave = Leave.builder()
                .dateDebut(LocalDate.of(2024, 1, 10))
                .dateFin(LocalDate.of(2024, 1, 15))
                .type(LeaveType.MALADIE)
                .build();

        leave.prePersist();

        assertEquals(LeaveStatus.PENDING, leave.getStatus());
        assertNotNull(leave.getDateSoumission());
        assertEquals(6, leave.getNombreJours()); // 10 → 15 inclusive = 6 days
    }

    @Test
    void prePersist_SingleDay_NombreJoursOne() {
        Leave leave = Leave.builder()
                .dateDebut(LocalDate.of(2024, 3, 1))
                .dateFin(LocalDate.of(2024, 3, 1)) // same day
                .type(LeaveType.TELETRAVAIL)
                .build();

        leave.prePersist();

        assertEquals(1, leave.getNombreJours());
        assertEquals(LeaveStatus.PENDING, leave.getStatus());
    }

    @Test
    void prePersist_DateSoumissionIsToday() {
        Leave leave = Leave.builder()
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(2))
                .type(LeaveType.CONGE_ANNUEL)
                .build();

        leave.prePersist();

        assertEquals(LocalDate.now(), leave.getDateSoumission());
    }

    @Test
    void leave_Builder_SetsAllFields() {
        User employee = new User();
        employee.setId(1L);

        Leave leave = Leave.builder()
                .id(1L)
                .employee(employee)
                .type(LeaveType.CONGE_ANNUEL)
                .status(LeaveStatus.APPROVED)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(5))
                .nombreJours(6)
                .motif("Vacation")
                .build();

        assertEquals(1L, leave.getId());
        assertEquals(employee, leave.getEmployee());
        assertEquals(LeaveType.CONGE_ANNUEL, leave.getType());
        assertEquals(LeaveStatus.APPROVED, leave.getStatus());
        assertEquals(6, leave.getNombreJours());
        assertEquals("Vacation", leave.getMotif());
    }

    @Test
    void leaveStatus_AllValues() {
        assertNotNull(LeaveStatus.PENDING);
        assertNotNull(LeaveStatus.APPROVED);
        assertNotNull(LeaveStatus.REJECTED);
        assertNotNull(LeaveStatus.CANCELLED);
        assertEquals(4, LeaveStatus.values().length);
    }

    @Test
    void leaveType_AllValues() {
        assertNotNull(LeaveType.CONGE_ANNUEL);
        assertNotNull(LeaveType.MALADIE);
        assertNotNull(LeaveType.TELETRAVAIL);
        assertTrue(LeaveType.values().length >= 3);
    }
}
