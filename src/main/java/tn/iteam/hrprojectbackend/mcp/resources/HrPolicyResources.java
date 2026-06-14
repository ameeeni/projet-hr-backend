package tn.iteam.hrprojectbackend.mcp.resources;

import org.springframework.stereotype.Component;

@Component
public class HrPolicyResources {

    public String leavePolicy() {
        return """
                Politique de congés :
                - Le congé annuel doit être demandé à l'avance.
                - Le congé maladie peut nécessiter un justificatif.
                - Le télétravail est soumis à validation du manager.
                """;
    }
}