package com.sportscenter.chat;
import jakarta.validation.constraints.Size;
public record ChatConversationRequest(@Size(max=200) String subject) { }
