package com.sportscenter.common;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RuntimeIdentityTest {
    @Test void usesEffectivePoolTargetInsteadOfPlaceholderEnvironmentWithoutConnecting() throws Exception {
        var pool = mock(HikariDataSource.class);
        when(pool.getJdbcUrl()).thenReturn("jdbc:postgresql://effective.invalid:5432/postgres?sslmode=require&password=not-for-output");
        when(pool.getUsername()).thenReturn("tenant-user");
        var env = new MockEnvironment().withProperty("DB_URL", "jdbc:postgresql://different.invalid:5432/postgres")
                .withProperty("RENDER_GIT_COMMIT", "ABCDEF1234567890ABCDEF1234567890ABCDEF12");
        var response = new HealthCheckController(new RuntimeIdentity(env, pool)).ping();
        var body = response.getBody();
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest("effective.invalid:5432//postgres|tenant-user".getBytes(StandardCharsets.UTF_8)));
        assertEquals(expected, body.get("databaseTargetFingerprint"));
        assertEquals("abcdef1234567890abcdef1234567890abcdef12", body.get("buildCommit"));
        assertEquals("UP", body.get("status")); assertEquals("pong", body.get("message"));
        assertEquals("no-store", response.getHeaders().getFirst("Cache-Control"));
        assertFalse(body.toString().contains("effective.invalid"));
        assertFalse(body.toString().contains("tenant-user"));
        assertFalse(body.toString().contains("not-for-output"));
        verify(pool, never()).getConnection();
    }

    @Test void embeddedCredentialsAreNeverEchoed() throws Exception {
        var pool = mock(HikariDataSource.class);
        when(pool.getJdbcUrl()).thenReturn("jdbc:postgresql://hidden-user:hidden-password@db.invalid:5432/postgres");
        var identity = new RuntimeIdentity(new MockEnvironment().withProperty("RENDER_GIT_COMMIT", "invalid-secret-value"), pool);
        assertEquals("unavailable", identity.values().get("databaseTargetFingerprint"));
        assertEquals("unknown", identity.values().get("buildCommit"));
        assertFalse(identity.values().toString().contains("hidden-"));
        verify(pool, never()).getConnection();
    }

    @Test void unsupportedDataSourceKeepsPingAvailableWithoutQueryingIt() {
        var source = mock(DataSource.class);
        var body = new HealthCheckController(new RuntimeIdentity(new MockEnvironment(), source)).ping().getBody();
        assertEquals("pong", body.get("message")); assertEquals("unavailable", body.get("databaseTargetFingerprint"));
        assertEquals("1", body.get("diagnosticsVersion")); verifyNoInteractions(source);
    }

    @Test void brokenPoolMetadataDoesNotExposeExceptionOrBreakPing() {
        var source = mock(HikariDataSource.class);
        when(source.getJdbcUrl()).thenThrow(new IllegalStateException("sensitive-internal-message"));
        var values = new RuntimeIdentity(new MockEnvironment(), source).values();
        assertEquals("unavailable", values.get("databaseTargetFingerprint"));
        assertFalse(values.toString().contains("sensitive"));
    }
}
