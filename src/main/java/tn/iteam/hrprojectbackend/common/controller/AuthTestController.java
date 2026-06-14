package tn.iteam.hrprojectbackend.common.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iteam.hrprojectbackend.leave.services.LeaveService;
import tn.iteam.hrprojectbackend.users.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Tag(name = "Debug", description = "Endpoints de debug pour tester l'authentification")
public class AuthTestController {

    private final UserRepository userRepository;
    private final LeaveService leaveService;

    /**
     * Endpoint pour voir les informations d'authentification actuelles
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return ResponseEntity.ok(Map.of("error", "Aucune authentification trouvée"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("username", auth.getName());
        result.put("authenticated", auth.isAuthenticated());
        result.put("authorities", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        result.put("principal", auth.getPrincipal());
        result.put("details", auth.getDetails());

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pour tester la vérification de propriété
     */
    @GetMapping("/check-owner/{employeeId}")
    public ResponseEntity<Map<String, Object>> checkOwner(@PathVariable Long employeeId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return ResponseEntity.ok(Map.of("error", "Aucune authentification trouvée"));
        }

        String username = auth.getName();
        boolean isOwner = leaveService.isOwner(username, employeeId);

        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        boolean isEmployee = authorities.contains("ROLE_EMPLOYEE");

        Map<String, Object> result = new HashMap<>();
        result.put("username", username);
        result.put("employeeId", employeeId);
        result.put("isOwner", isOwner);
        result.put("authorities", authorities);
        result.put("isEmployee", isEmployee);
        result.put("wouldBeAllowed", !isEmployee || isOwner);

        // Chercher l'utilisateur par email
        userRepository.findByEmail(username).ifPresentOrElse(
                user -> {
                    result.put("userFound", true);
                    result.put("userId", user.getId());
                    result.put("userEmail", user.getEmail());
                    result.put("userRole", user.getRole());
                },
                () -> result.put("userFound", false)
        );

        return ResponseEntity.ok(result);
    }

    /**
     * Endpoint pour lister tous les employés (IDs disponibles)
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(user -> Map.of(
                                "id", (Object) user.getId(),
                                "email", user.getEmail(),
                                "matricule", user.getMatricule(),
                                "nom", user.getNom(),
                                "prenom", user.getPrenom(),
                                "role", user.getRole().toString()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
