package com.resumeai.resume.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Spring AI ChatClient bean used for Groq API calls.
 * Follows the pattern from PetclinicChatClient in petclinic-genai-service.
 */
@Configuration
public class AIConfiguration {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
