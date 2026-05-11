package com.marcog.peluqueria.chatbot.domain;

import lombok.Data;

@Data
public class ChatMessageDto {
    private String role;
    private String content;
}
