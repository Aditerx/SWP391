package com.sportscenter.chat.ai;

import java.util.regex.Pattern;

final class ChatSensitiveDataFilter {
    private static final Pattern LABELED_SECRET = Pattern.compile("(?i)\\b(password|passcode|passphrase|mật khẩu|otp|one[- ]time code|verification code|mã xác minh)(?:\\s+(?:code|is|là))?\\s*[:：=]?\\s*[^\\s,;]{3,}");
    private static final Pattern BEARER = Pattern.compile("(?i)\\b(bearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Pattern KEY_OR_TOKEN = Pattern.compile("(?i)\\b(api[_ -]?key|token)\\s*[:=]\\s*[^\\s,;]{8,}");
    private static final Pattern ANTHROPIC_KEY = Pattern.compile("\\bsk-ant-[A-Za-z0-9_-]{20,}\\b");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern CARD = Pattern.compile("(?<!\\d)(?:\\d[ -]?){8,19}(?!\\d)");
    private static final Pattern CARD_LABEL = Pattern.compile("(?i)\\b(card number|số thẻ|cvv|cvc)\\s*(?:is|[:=])?\\s*[0-9 -]{3,24}");

    private ChatSensitiveDataFilter() { }

    static String redact(String input) {
        if (LABELED_SECRET.matcher(input).find() || BEARER.matcher(input).find()
                || KEY_OR_TOKEN.matcher(input).find() || ANTHROPIC_KEY.matcher(input).find()
                || CARD_LABEL.matcher(input).find() || JWT.matcher(input).find() || CARD.matcher(input).find()) {
            return "[message omitted because it may contain sensitive information]";
        }
        return input;
    }
}
