package tn.iteam.hrprojectbackend.chat.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.iteam.hrprojectbackend.chat.dto.ChatRequest;
import tn.iteam.hrprojectbackend.chat.dto.ChatResponse;
import tn.iteam.hrprojectbackend.chat.service.ChatService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock private ChatService chatService;

    @InjectMocks private ChatController chatController;

    @Test
    void chat_ReturnsResponse() {
        ChatRequest request = new ChatRequest("Quel est mon solde de congés?");
        when(chatService.ask("Quel est mon solde de congés?")).thenReturn("Votre solde est de 15 jours.");

        ChatResponse result = chatController.chat(request);

        assertNotNull(result);
        assertEquals("Votre solde est de 15 jours.", result.answer());
    }

    @Test
    void chat_EmptyResponse() {
        ChatRequest request = new ChatRequest("Test message");
        when(chatService.ask("Test message")).thenReturn("");

        ChatResponse result = chatController.chat(request);

        assertNotNull(result);
        assertEquals("", result.answer());
    }
}
