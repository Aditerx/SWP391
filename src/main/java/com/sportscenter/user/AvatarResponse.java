package com.sportscenter.user;

public record AvatarResponse(String avatarUrl) {
    public AvatarResponse {
        avatarUrl = com.sportscenter.media.AvatarUrlOptimizer.optimize(avatarUrl);
    }
}
