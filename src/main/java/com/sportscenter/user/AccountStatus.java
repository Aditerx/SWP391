package com.sportscenter.user;

import com.sportscenter.common.exception.BusinessException;
import java.util.Locale;

/** Explicit account commands; creation defaults are applied by the caller. */
final class AccountStatus {
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

    private static BusinessException invalid() {
        return new BusinessException("INVALID_ACCOUNT_STATUS: Supply Active, Inactive or Locked (Suspended is an alias of Locked)");
    }
}
