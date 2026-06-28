package tn.iteam.hrprojectbackend.infrastructure.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tn.iteam.hrprojectbackend.infrastructure.kafka.Producer.HrEventProducer;
import tn.iteam.hrprojectbackend.leave.entities.LeaveStatus;
import tn.iteam.hrprojectbackend.leave.entities.LeaveType;
import tn.iteam.hrprojectbackend.leave.events.LeaveSubmittedEvent;
import tn.iteam.hrprojectbackend.leave.events.LeaveValidatedEvent;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.events.EmployeeCreatedEvent;
import tn.iteam.hrprojectbackend.users.events.UserEventDto;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HrEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private HrEventProducer hrEventProducer;

    @Test
    void sendUserCreated_SendsToCorrectTopic() {
        UserEventDto event = new UserEventDto(
                "EMP001", "Doe", "John", "john@test.com",
                "pass", "Developer", "IT", null, 30, null);

        hrEventProducer.sendUserCreated(event);

        verify(kafkaTemplate).send("user-created", "EMP001", event);
    }

    @Test
    void sendLeaveSubmitted_SendsToCorrectTopic() {
        LeaveSubmittedEvent event = LeaveSubmittedEvent.builder()
                .leaveRequestId(1L)
                .employeeId(10L)
                .employeeNom("Doe")
                .employeeEmail("john@test.com")
                .type(LeaveType.MALADIE)
                .dateDebut(LocalDate.now())
                .dateFin(LocalDate.now().plusDays(2))
                .nombreJours(3)
                .motif("Sick")
                .build();

        hrEventProducer.sendLeaveSubmitted(event);

        verify(kafkaTemplate).send("leave-submitted", "1", event);
    }

    @Test
    void sendLeaveValidated_SendsToCorrectTopic() {
        LeaveValidatedEvent event = LeaveValidatedEvent.builder()
                .leaveRequestId(1L)
                .employeeId(10L)
                .employeeEmail("john@test.com")
                .decision(LeaveStatus.APPROVED)
                .commentaire("Approved")
                .validatedByNom("Manager")
                .build();

        hrEventProducer.sendLeaveValidated(event);

        verify(kafkaTemplate).send("leave-validated", "1", event);
    }

    @Test
    void sendEmployeeCreated_SendsToCorrectTopic() {
        EmployeeCreatedEvent event = EmployeeCreatedEvent.builder()
                .employeeId(1L)
                .nom("Doe")
                .email("john@test.com")
                .poste("Developer")
                .role(Role.EMPLOYEE)
                .build();

        hrEventProducer.sendEmployeeCreated(event);

        verify(kafkaTemplate).send("employee-created", "1", event);
    }
}
