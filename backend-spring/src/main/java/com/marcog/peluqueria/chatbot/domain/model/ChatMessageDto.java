package com.marcog.peluqueria.chatbot.domain.model;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String role;
    private String content;
}
