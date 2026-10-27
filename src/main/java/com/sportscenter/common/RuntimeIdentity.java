package com.sportscenter.common;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/** Non-secret identity only. Reads effective pool settings without opening a database connection. */
@Component
public class RuntimeIdentity {
    private final Map<String, String> values;

    public RuntimeIdentity(Environment environment, DataSource dataSource) {
        String commit = environment.getProperty("RENDER_GIT_COMMIT", "");
        commit = commit.matches("[a-fA-F0-9]{7,64}") ? commit.toLowerCase(Locale.ROOT) : "unknown";
        values = Map.of("diagnosticsVersion", "1", "buildCommit", commit,
                "databaseTargetFingerprint", fingerprint(dataSource));
    }

    public Map<String, String> values() { return values; }

    private static String fingerprint(DataSource source) {
        if (!(source instanceof HikariDataSource pool)) return "unavailable";
        try {
            String jdbcUrl = pool.getJdbcUrl();
            if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:postgresql://")) return "unavailable";
            URI target = URI.create(jdbcUrl.substring(5));
            if (target.getHost() == null || target.getUserInfo() != null) return "unavailable";
            String identity = target.getHost() + ":" + target.getPort() + "/" + target.getPath() + "|" + pool.getUsername();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return "unavailable";
        }
    }
}
