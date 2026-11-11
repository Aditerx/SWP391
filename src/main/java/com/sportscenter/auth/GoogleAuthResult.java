package com.sportscenter.auth;

import com.sportscenter.user.User;

public record GoogleAuthResult(User user, boolean newAccount) { }
