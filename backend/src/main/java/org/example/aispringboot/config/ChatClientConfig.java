package org.example.aispringboot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI ChatClient 配置
 * 注册一个带对话记忆（MessageChatMemoryAdvisor）的 ChatClient，
 * 系统提示词（人设）统一由 PromptManage 提供并在调用处注入，
 * 避免在多处重复定义。
 */
@Configuration
public class ChatClientConfig {

    /**
     * 基于内存的对话记忆，每个会话最多保留最近 30 条消息。
     * 注意：内存实现重启后会丢失历史，生产环境可替换为持久化实现。
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(30)
                .build();
    }

    /**
     * 注册 OpenAI 协议兼容的 ChatClient，默认挂载对话记忆 Advisor。
     */
    @Bean("open-ai")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
