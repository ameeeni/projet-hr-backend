package tn.iteam.hrprojectbackend.infrastructure.kafka.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import tn.iteam.hrprojectbackend.dashboard.services.DashboardService;
import tn.iteam.hrprojectbackend.leave.events.LeaveSubmittedEvent;
import tn.iteam.hrprojectbackend.leave.events.LeaveValidatedEvent;
import tn.iteam.hrprojectbackend.users.entities.Role;
import tn.iteam.hrprojectbackend.users.entities.User;
import tn.iteam.hrprojectbackend.users.events.EmployeeCreatedEvent;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor

public class HrEventConsumer {
    private final DashboardService dashboardService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "user-created", groupId = "hr-group")
    public void handleUserCreated(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            log.info("📨 Message brut reçu du topic {}: {}", topic, message);

            // Parser le JSON manuellement pour plus de flexibilité
            JsonNode jsonNode = objectMapper.readTree(message);

            String matricule = jsonNode.has("username") ? jsonNode.get("username").asText() :
                              (jsonNode.has("matricule") ? jsonNode.get("matricule").asText() : null);

            String nom = jsonNode.has("lastName") ? jsonNode.get("lastName").asText() :
                        (jsonNode.has("nom") ? jsonNode.get("nom").asText() : null);

            String prenom = jsonNode.has("firstName") ? jsonNode.get("firstName").asText() :
                           (jsonNode.has("prenom") ? jsonNode.get("prenom").asText() : null);

            String email = jsonNode.has("email") ? jsonNode.get("email").asText() : null;

            // Le password n'est pas envoyé par IAM, générer un défaut
            String password = jsonNode.has("password") ? jsonNode.get("password").asText() : "$2a$10$default";

            String poste = jsonNode.has("poste") ? jsonNode.get("poste").asText() : "Non défini";
            String departement = jsonNode.has("departement") ? jsonNode.get("departement").asText() : "Non défini";
            Integer soldeConge = jsonNode.has("soldeConge") ? jsonNode.get("soldeConge").asInt() : 30;

            // Parser la date
            LocalDate dateEmbauche = LocalDate.now();
            if (jsonNode.has("createdAt") && !jsonNode.get("createdAt").isNull()) {
                try {
                    // IAM envoie createdAt en LocalDateTime, on prend juste la date
                    String createdAtStr = jsonNode.get("createdAt").asText();
                    dateEmbauche = LocalDate.parse(createdAtStr.substring(0, 10));
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de parser createdAt, utilisation de la date actuelle");
                }
            } else if (jsonNode.has("dateEmbauche") && !jsonNode.get("dateEmbauche").isNull()) {
                try {
                    dateEmbauche = LocalDate.parse(jsonNode.get("dateEmbauche").asText());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de parser dateEmbauche, utilisation de la date actuelle");
                }
            }

            // Mapper le rôle IAM vers le rôle HR
            Role role = Role.EMPLOYEE; // Par défaut
            if (jsonNode.has("role")) {
                String roleStr = jsonNode.get("role").asText();
                role = mapIamRoleToHrRole(roleStr);
            }

            log.info("👤 Réception d'un nouvel utilisateur: username={}, nom={} {}, email={}, role={}",
                    matricule, prenom, nom, email, role);

            // Vérifier si l'utilisateur existe déjà par email ou matricule
            if (matricule != null && userRepository.existsByMatricule(matricule)) {
                log.warn("⚠️ L'utilisateur avec le matricule {} existe déjà", matricule);
                return;
            }

            if (email != null && userRepository.existsByEmail(email)) {
                log.warn("⚠️ L'utilisateur avec l'email {} existe déjà", email);
                return;
            }

            User user = new User();
            user.setMatricule(matricule);
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setEmail(email);
            user.setPassword(password);
            user.setPoste(poste);
            user.setDepartement(departement);
            user.setDateEmbauche(dateEmbauche);
            user.setSoldeConge(soldeConge);
            user.setRole(role);

            userRepository.save(user);
            log.info("✅ Utilisateur créé avec succès dans HR: id={}, matricule={}, email={}",
                    user.getId(), user.getMatricule(), user.getEmail());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur: {}", e.getMessage(), e);
            log.error("📄 Message qui a causé l'erreur: {}", message);
            // Ne pas relancer l'exception pour éviter les retry infinis
        }
    }

    /**
     * Mapper les rôles IAM vers les rôles HR
     * IAM utilise: ROLE_ADMIN, ROLE_MANAGER, ROLE_HR, ROLE_EMPLOYEE
     * HR utilise: MANAGER, HR, EMPLOYEE
     */
    private Role mapIamRoleToHrRole(String iamRole) {
        if (iamRole == null) {
            return Role.EMPLOYEE;
        }

        // Nettoyer le rôle (enlever ROLE_ si présent)
        String cleanRole = iamRole.replace("ROLE_", "").toUpperCase();

        return switch (cleanRole) {
            case "ADMIN", "RH", "HR" -> Role.HR;
            case "MANAGER" -> Role.MANAGER;
            case "EMPLOYEE", "EMPLOYE" -> Role.EMPLOYEE;
            default -> {
                log.warn("Rôle IAM inconnu: {}, utilisation de EMPLOYEE par défaut", iamRole);
                yield Role.EMPLOYEE;
            }
        };
    }
    @KafkaListener(topics = "leave-submitted", groupId = "hr-group")
    public void onLeaveSubmitted(LeaveSubmittedEvent event) {
        log.info("<<< Reçu leave-submitted pour : {}", event.getEmployeeNom());
        // → notifier le manager
        dashboardService.refreshStats();

    }

    @KafkaListener(topics = "leave-validated", groupId = "hr-group")
    public void onLeaveValidated(LeaveValidatedEvent event) {
        log.info("<<< Reçu leave-validated : {}", event.getDecision());
        // → notifier l'employé de la décision
        dashboardService.refreshStats();

    }

    @KafkaListener(topics = "employee-created", groupId = "hr-group")
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        log.info("<<< Reçu employee-created : {}", event.getNom());
        // → email de bienvenue
        dashboardService.refreshStats();

    }
}