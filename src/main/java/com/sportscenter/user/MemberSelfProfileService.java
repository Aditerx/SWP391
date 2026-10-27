package com.sportscenter.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportscenter.common.exception.ResourceNotFoundException;
import com.sportscenter.user.dto.MemberSelfProfilePatch;
import com.sportscenter.user.dto.MemberSelfProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MemberSelfProfileService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberSelfProfileResponse get(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        requireActiveMember(user);
        Member member = memberRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile was not found"));
        return MemberSelfProfileResponse.from(user, member);
    }

    @Transactional
    public MemberSelfProfileResponse patch(String email, MemberSelfProfilePatch patch) {
        // Serialize PATCH read-modify-write before loading either profile row.
        User user = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        requireActiveMember(user);
        Member member = memberRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile was not found"));
        Map<String, JsonNode> fields = patch.fields();

        if (fields.containsKey("name")) user.setFullName(requiredName(fields.get("name")));
        if (fields.containsKey("phone")) user.setPhone(nullableText("phone", fields.get("phone"), 20));
        if (fields.containsKey("address")) user.setAddress(nullableText("address", fields.get("address"), 255));
        if (fields.containsKey("dateOfBirth")) user.setDateOfBirth(nullableDateOfBirth(fields.get("dateOfBirth")));
        if (fields.containsKey("gender")) user.setGender(nullableGender(fields.get("gender")));
        if (fields.containsKey("goal")) member.setGoal(nullableText("goal", fields.get("goal"), 255));
        if (fields.containsKey("healthNote")) member.setHealthNote(nullableText("healthNote", fields.get("healthNote"), Integer.MAX_VALUE));

        userRepository.save(user);
        memberRepository.save(member);
        return MemberSelfProfileResponse.from(user, member);
    }

    private String requiredName(JsonNode value) {
        if (value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()) {
            throw invalid("NAME_REQUIRED", "name must be a non-blank string");
        }
        if (value.asText().length() > 100) throw invalid("NAME_TOO_LONG", "name must be at most 100 characters");
        return value.asText();
    }

    private void requireActiveMember(User user) {
        if (!"Active".equalsIgnoreCase(user.getStatus()) || user.getRole() == null
                || !"Member".equalsIgnoreCase(user.getRole().getName())) {
            throw new org.springframework.security.access.AccessDeniedException("Active Member account required");
        }
    }

    private String nullableText(String field, JsonNode value, int maxLength) {
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalid("INVALID_" + field.toUpperCase(Locale.ROOT), field + " must be a string or null");
        if (value.asText().length() > maxLength) {
            throw invalid(field.toUpperCase(Locale.ROOT) + "_TOO_LONG", field + " must be at most " + maxLength + " characters");
        }
        return value.asText();
    }

    private LocalDate nullableDateOfBirth(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (!value.isTextual()) throw invalid("INVALID_DATE_OF_BIRTH", "dateOfBirth must use yyyy-MM-dd or null");
        try {
            LocalDate date = LocalDate.parse(value.asText());
            if (!date.toString().equals(value.asText())) {
                throw invalid("INVALID_DATE_OF_BIRTH", "dateOfBirth must use yyyy-MM-dd");
            }
            if (date.isAfter(LocalDate.now(clock))) {
                throw invalid("FUTURE_DATE_OF_BIRTH", "dateOfBirth cannot be in the future");
            }
            return date;
        } catch (DateTimeParseException ex) {
            throw invalid("INVALID_DATE_OF_BIRTH", "dateOfBirth must use yyyy-MM-dd");
        }
    }

    private String nullableGender(JsonNode value) {
        String gender = nullableText("gender", value, 10);
        if (gender != null && !Set.of("Male", "Female", "Other").contains(gender)) {
            throw invalid("INVALID_GENDER", "gender must be Male, Female, Other, or null");
        }
        return gender;
    }

    private com.sportscenter.common.exception.BusinessException invalid(String code, String message) {
        return new com.sportscenter.common.exception.BusinessException(code + ": " + message);
    }
}
