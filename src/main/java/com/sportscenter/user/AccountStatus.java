package com.sportscenter.user;

import com.sportscenter.common.exception.BusinessException;
import java.util.Locale;

/** Explicit account commands; creation defaults are applied by the caller. */
public final class AccountStatus {
    public enum AuthenticationState { ACTIVE, PENDING, BLOCKED }

    private AccountStatus() { }

    static String require(String status) {
        if (status == null) throw invalid();
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "active" -> "Active";
            case "inactive" -> "Inactive";
            case "locked", "suspended" -> "Locked";
            default -> throw invalid();
        };
    }

    /** Maps persisted states to safe Google authentication outcomes; unknown states are blocked. */
    public static AuthenticationState forAuthentication(String status) {
        if (status == null) return AuthenticationState.BLOCKED;
        return switch (status.trim().toLowerCase(Locale.ROOT)) {
            case "active" -> AuthenticationState.ACTIVE;
            case "pending" -> AuthenticationState.PENDING;
            case "inactive", "locked", "suspended" -> AuthenticationState.BLOCKED;
            default -> AuthenticationState.BLOCKED;
        };
    }

    private static BusinessException invalid() {
        return new BusinessException("INVALID_ACCOUNT_STATUS: Supply Active, Inactive or Locked (Suspended is an alias of Locked)");
    }
}
