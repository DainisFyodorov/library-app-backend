package com.dainis.spring_boot_library.service;

import com.dainis.spring_boot_library.dao.MessageRepository;
import com.dainis.spring_boot_library.entity.Message;
import com.dainis.spring_boot_library.requestmodels.AdminQuestionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessagesServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessagesService messagesService;

    @Test
    void testPostMessageSuccess() {
        String userEmail = "test@example.com";
        Message messageRequest = new Message("Test title", "Test question");

        messagesService.postMessage(messageRequest, userEmail);

        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void testPutMessageSuccess() throws Exception {
        String adminEmail = "admin@example.com";
        Long messageId = 1L;

        Message existingMessage = new Message("Test title", "Test question");
        existingMessage.setId(messageId);
        existingMessage.setClosed(false);

        AdminQuestionRequest adminQuestionRequest = new AdminQuestionRequest();
        adminQuestionRequest.setId(messageId);
        adminQuestionRequest.setResponse("Test response");

        when(messageRepository.findById(existingMessage.getId())).thenReturn(Optional.of(existingMessage));

        assertDoesNotThrow(() -> messagesService.putMessage(adminQuestionRequest, adminEmail));
        assertEquals(adminQuestionRequest.getResponse(), existingMessage.getResponse());
        assertEquals(adminEmail, existingMessage.getAdminEmail());
        assertTrue(existingMessage.isClosed());

        verify(messageRepository, times(1)).save(existingMessage);
    }
}
