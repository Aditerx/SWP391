package com.sportscenter.user.dto;

import com.sportscenter.user.Member;
import com.sportscenter.user.User;

import java.time.LocalDate;

public record MemberSelfProfileResponse(
        String name,
        String phone,
        String address,
        LocalDate dateOfBirth,
        String gender,
        String goal,
        String healthNote
) {
    public static MemberSelfProfileResponse from(User user, Member member) {
        return new MemberSelfProfileResponse(user.getFullName(), user.getPhone(), user.getAddress(),
                user.getDateOfBirth(), user.getGender(), member.getGoal(), member.getHealthNote());
    }
}
