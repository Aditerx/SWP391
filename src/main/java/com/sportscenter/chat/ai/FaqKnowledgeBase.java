package com.sportscenter.chat.ai;

import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

/** Deterministic, local FAQ matching; never calls an external service. */
final class FaqKnowledgeBase {
    private static final Set<String> STOPWORDS = Set.of("co", "la", "va", "trong", "ngoai", "tai", "lieu", "cua", "cho", "toi", "ban", "ve", "gi", "nao", "nhu", "the", "nay", "duoc", "khong", "cac", "nhung");
    private final List<Section> sections;

    FaqKnowledgeBase() {
        this(readKnowledge());
    }

    FaqKnowledgeBase(String raw) {
        String text = repairMojibake(raw == null ? "" : raw);
        List<Section> parsed = new ArrayList<>();
        String title = null;
        List<String> keywords = new ArrayList<>();
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (line.startsWith("## ")) {
                if (title != null) parsed.add(section(title, keywords, body));
                title = line.substring(3).trim();
                keywords = new ArrayList<>();
                body = new StringBuilder();
            } else if (title != null && normalize(line).startsWith(normalize("T\u1eeb kh\u00f3a:"))) {
                String rawKeywords = line.substring(line.indexOf(':') + 1);
                Arrays.stream(rawKeywords.split("[,;]"))
                        .map(String::trim).filter(value -> !value.isBlank()).forEach(keywords::add);
            } else if (title != null) {
                if (!body.isEmpty()) body.append('\n');
                body.append(line);
            }
        }
        if (title != null) parsed.add(section(title, keywords, body));
        this.sections = List.copyOf(parsed);
    }

    List<String> topics(int max) {
        return sections.stream().limit(Math.max(0, max)).map(Section::title).toList();
    }

    Optional<String> answer(String query, int minimumScore, int maxCharacters) {
        String normalizedQuery = normalize(query);
        Set<String> queryTokens = tokens(normalizedQuery);
        Section best = null;
        int bestScore = 0;
        for (Section section : sections) {
            int score = 0;
            Set<String> keywordTokens = new HashSet<>();
            for (String keyword : section.keywords()) {
                String normalizedKeyword = normalize(keyword);
                if (normalizedKeyword.isBlank()) continue;
                keywordTokens.addAll(tokens(normalizedKeyword));
                if (containsPhrase(normalizedQuery, normalizedKeyword)) score += 2;
            }
            score += (int) queryTokens.stream().filter(token -> !STOPWORDS.contains(token) && keywordTokens.contains(token)).count();
            if (score > bestScore) {
                best = section;
                bestScore = score;
            }
        }
        if (best == null || bestScore < Math.max(1, minimumScore)) return Optional.empty();
        String answer = best.body().trim().replaceAll("(?m)^#+\\s*", "");
        int limit = Math.max(1, maxCharacters);
        if (answer.length() > limit) answer = answer.substring(0, limit).trim();
        return answer.isBlank() ? Optional.empty() : Optional.of(answer);
    }

    String topicList() {
        return String.join(", ", topics(5));
    }

    static String normalize(String value) {
        String decomposed = Normalizer.normalize(repairMojibake(value == null ? "" : value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }

    static String repairMojibake(String value) {
        if (value == null || !(value.contains("\u00c3") || value.contains("\u00e1\u00bb") || value.contains("\u00c4\u0111"))) return value;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
            for (char character : value.toCharArray()) {
                if (character <= 0xff) bytes.write(character);
                else {
                    int mapped = switch (character) {
                        case '\u20ac' -> 0x80; case '\u201a' -> 0x82; case '\u0192' -> 0x83;
                        case '\u201e' -> 0x84; case '\u2026' -> 0x85; case '\u2020' -> 0x86;
                        case '\u2021' -> 0x87; case '\u02c6' -> 0x88; case '\u2030' -> 0x89;
                        case '\u0160' -> 0x8a; case '\u2039' -> 0x8b; case '\u0152' -> 0x8c;
                        case '\u017d' -> 0x8e; case '\u2018' -> 0x91; case '\u2019' -> 0x92;
                        case '\u201c' -> 0x93; case '\u201d' -> 0x94; case '\u2022' -> 0x95;
                        case '\u2013' -> 0x96; case '\u2014' -> 0x97; case '\u02dc' -> 0x98;
                        case '\u2122' -> 0x99; case '\u0161' -> 0x9a; case '\u203a' -> 0x9b;
                        case '\u0153' -> 0x9c; case '\u017e' -> 0x9e; case '\u0178' -> 0x9f;
                        default -> -1;
                    };
                    if (mapped < 0) return value;
                    bytes.write(mapped);
                }
            }
            String decoded = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            int oldMarkers = markerCount(value), newMarkers = markerCount(decoded);
            return newMarkers < oldMarkers ? decoded : value;
        } catch (Exception ignored) {
            return value;
        }
    }

    private static int markerCount(String value) {
        return value.length() - value.replace("\u00c3", "").length()
                + value.length() - value.replace("\u00e1\u00bb", "").length()
                + value.length() - value.replace("\u00c4\u0111", "").length();
    }

    private static boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private static Set<String> tokens(String normalized) {
        if (normalized.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.asList(normalized.split(" ")));
    }

    private static Section section(String title, List<String> keywords, StringBuilder body) {
        List<String> resolved = keywords.isEmpty()
                ? Arrays.asList(title.split("\\s+")) : List.copyOf(keywords);
        return new Section(title, resolved, body.toString());
    }

    private static String readKnowledge() {
        try {
            return new ClassPathResource("ai/knowledge-base.md").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private record Section(String title, List<String> keywords, String body) { }
}
