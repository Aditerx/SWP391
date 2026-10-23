package com.sportscenter.specialization;

import com.sportscenter.user.Coach;

import java.util.List;

public record CoachSummaryResponse(Integer id, String name, String avatarUrl,
                                  List<String> specializations, String email, String phone,
                                  String bio, String certification, String status) {
    public static CoachSummaryResponse from(Coach coach, List<String> names, boolean showContacts) {
        return new CoachSummaryResponse(coach.getUserId(), coach.getUser().getFullName(), coach.getUser().getAvatarUrl(),
                names, showContacts ? coach.getUser().getEmail() : null,
                showContacts ? coach.getUser().getPhone() : null,
                coach.getBio(), coach.getCertification(), coach.getUser().getStatus());
    }
}
