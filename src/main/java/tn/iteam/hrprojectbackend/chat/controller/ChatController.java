package tn.iteam.hrprojectbackend.chat.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.iteam.hrprojectbackend.chat.dto.ChatRequest;
import tn.iteam.hrprojectbackend.chat.dto.ChatResponse;
import tn.iteam.hrprojectbackend.chat.service.ChatService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat IA", description = "Assistant RH intelligent")
public class ChatController {
    private final ChatService chatService;

    // Chat IA : accessible à tous les rôles authentifiés
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'MANAGER', 'HR')")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return new ChatResponse(chatService.ask(request.message()));
    }
}
