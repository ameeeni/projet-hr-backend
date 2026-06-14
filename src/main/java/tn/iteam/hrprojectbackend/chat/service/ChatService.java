package tn.iteam.hrprojectbackend.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tn.iteam.hrprojectbackend.mcp.tools.EmployeeTools;
import tn.iteam.hrprojectbackend.mcp.tools.HRTools;
import tn.iteam.hrprojectbackend.mcp.tools.ManagerTools;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatClient chatClient;
    private final EmployeeTools employeeTools;
    private final ManagerTools managerTools;
    private final HRTools hrTools;

    public String ask(String userMessage) {
        return chatClient.prompt()
                .system("""
                        Tu es un assistant RH intelligent.
                        Utilise les tools disponibles quand une donnée métier est nécessaire.
                        N'invente jamais les soldes de congé, statistiques ou historiques.
                        Réponds en français.
                        """)
                .user(userMessage)
                .tools(employeeTools, managerTools, hrTools)
                .call()
                .content();
    }
}
