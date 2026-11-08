package com.sportscenter.chat.ai;

public record AiCompletion(String text, String model, Integer inputTokens, Integer outputTokens) { }
