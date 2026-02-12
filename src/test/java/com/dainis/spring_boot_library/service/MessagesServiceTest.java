package com.dainis.spring_boot_library.service;

import com.dainis.spring_boot_library.dao.MessageRepository;
import com.dainis.spring_boot_library.entity.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        messageRequest.setUserEmail(userEmail);

        messagesService.postMessage(messageRequest, userEmail);

        verify(messageRepository, times(1)).save(any(Message.class));
    }
}
