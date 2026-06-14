package tn.iteam.hrprojectbackend.infrastructure.kafka.Producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tn.iteam.hrprojectbackend.leave.events.LeaveSubmittedEvent;
import tn.iteam.hrprojectbackend.leave.events.LeaveValidatedEvent;
import tn.iteam.hrprojectbackend.users.events.EmployeeCreatedEvent;
import tn.iteam.hrprojectbackend.users.events.UserEventDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class HrEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserCreated(UserEventDto event) {
        log.info(">>> Envoi user-created : {}", event);
        kafkaTemplate.send("user-created", event.getMatricule(), event);
    }

    public void sendLeaveSubmitted(LeaveSubmittedEvent event) {
        log.info(">>> Envoi leave-submitted : {}", event);
        kafkaTemplate.send("leave-submitted",
                String.valueOf(event.getLeaveRequestId()),event);
    }
    public void sendLeaveValidated(LeaveValidatedEvent event) {
        log.info(">>> Envoi leave-validated : {}", event);
        kafkaTemplate.send("leave-validated",
                String.valueOf(event.getLeaveRequestId()), event);
    }

    public void sendEmployeeCreated(EmployeeCreatedEvent event) {
        log.info(">>> Envoi employee-created : {}", event);
        kafkaTemplate.send("employee-created",
                String.valueOf(event.getEmployeeId()), event);
    }
}
