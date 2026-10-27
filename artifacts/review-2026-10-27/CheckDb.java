import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.net.URI;

public class CheckDb {
    private static String quote(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    public static void main(String[] args) throws Exception {
        Properties local = new Properties();
        try (var reader = new java.io.StringReader(Files.readString(Path.of(".env"), StandardCharsets.UTF_8).replaceFirst("^\\uFEFF", ""))) {
            local.load(reader);
        }
        String url = local.getProperty("DB_URL", "");
        if (!url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException("Expected PostgreSQL JDBC URL");
        boolean direct = args.length == 1 && "--direct-from-project".equals(args[0]);
        if (direct) {
            String user = local.getProperty("DB_USERNAME", "");
            if (!user.matches("postgres\\.[a-z0-9]{20}")) throw new IllegalArgumentException("Expected a project-scoped Supabase username");
            url = "jdbc:postgresql://db." + user.substring("postgres.".length()) + ".supabase.co:5432/postgres?sslmode=require";
        }
        URI parsedUrl;
        try {
            parsedUrl = URI.create(url.substring(5));
        } catch (IllegalArgumentException malformed) {
            System.out.println("{\"success\":false,\"configuration_error\":\"INVALID_JDBC_URL\"}");
            System.exit(3);
            return;
        }
        if (parsedUrl.getRawUserInfo() != null) {
            String[] supplied = parsedUrl.getUserInfo().split(":", 2);
            boolean sameUser = supplied[0].equals(local.getProperty("DB_USERNAME", ""));
            boolean samePassword = supplied.length == 2 && supplied[1].equals(local.getProperty("DB_PASSWORD", ""));
            System.out.println("{\"success\":false,\"host\":" + quote(parsedUrl.getHost())
                + ",\"configuration_error\":\"JDBC_URL_CONTAINS_USERINFO\",\"username_sources_match\":" + sameUser
                + ",\"password_sources_match\":" + samePassword + ",\"sql_executed\":false}");
            System.exit(3);
            return;
        }
        Properties cfg = new Properties();
        cfg.setProperty("user", direct ? "postgres" : local.getProperty("DB_USERNAME", ""));
        cfg.setProperty("password", local.getProperty("DB_PASSWORD", ""));
        cfg.setProperty("connectTimeout", "10");
        cfg.setProperty("socketTimeout", "15");
        cfg.setProperty("ApplicationName", "scms-readonly-connectcheck");
        if (!url.toLowerCase(Locale.ROOT).contains("sslmode=")) cfg.setProperty("sslmode", "require");
        Properties effective = org.postgresql.Driver.parseURL(url, cfg);
        String sslmode = effective == null ? "" : effective.getProperty("sslmode", "");
        if (!List.of("require", "verify-ca", "verify-full").contains(sslmode)) {
            System.out.println("{\"success\":false,\"configuration_error\":\"ENCRYPTED_JDBC_CONNECTION_REQUIRED\",\"sql_executed\":false}");
            System.exit(3);
            return;
        }
        DriverManager.setLoginTimeout(15);
        long began = System.nanoTime();
        String host = parsedUrl.getHost();
        String successReport;
        try (Connection conn = DriverManager.getConnection(url, cfg)) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);
            try (Statement check = conn.createStatement()) {
                check.setQueryTimeout(10);
                try (ResultSet rs = check.executeQuery("SHOW transaction_read_only")) {
                    rs.next();
                    if (!"on".equals(rs.getString(1))) throw new IllegalStateException("Read-only transaction was not enabled");
                }
                String query = "SELECT 1, current_database(), current_schema(), current_setting('server_version'), "
                    + "(SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()), "
                    + "(SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'), "
                    + "EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name='sessions' AND column_name='subscription_id')";
                try (ResultSet rs = check.executeQuery(query)) {
                    rs.next();
                    successReport = "{\"success\":true,\"host\":" + quote(host)
                        + ",\"select_one\":" + rs.getInt(1)
                        + ",\"database\":" + quote(rs.getString(2))
                        + ",\"schema\":" + quote(rs.getString(3))
                        + ",\"server_version\":" + quote(rs.getString(4))
                        + ",\"client_sslmode\":" + quote(sslmode)
                        + ",\"postgres_backend_tls\":" + rs.getObject(5)
                        + ",\"transaction_read_only\":true,\"public_tables\":" + rs.getInt(6)
                        + ",\"sessions_subscription_id_present\":" + rs.getBoolean(7)
                        + ",\"elapsed_ms\":" + ((System.nanoTime()-began)/1000000)
                        + ",\"rollback_completed\":true,\"method\":"
                        + quote("JDBC " + (direct ? "direct" : "Session pooler") + ", read-only metadata, rollback") + "}";
                }
            } finally {
                conn.rollback();
            }
        } catch (SQLException ex) {
            var causes = new ArrayList<String>();
            var reasons = new ArrayList<String>();
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) causes.add(quote(cause.getClass().getSimpleName()));
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                String reason = cause.getMessage();
                for (String key : List.of("DB_PASSWORD", "DB_USERNAME", "DB_URL")) {
                    String value = local.getProperty(key, "");
                    if (!value.isEmpty() && reason != null) reason = reason.replace(value, "[REDACTED]");
                }
                reasons.add(quote(reason));
            }
            String safeMessage = ex.getMessage();
            for (String key : List.of("DB_PASSWORD", "DB_USERNAME", "DB_URL")) {
                String value = local.getProperty(key, "");
                if (!value.isEmpty() && safeMessage != null) safeMessage = safeMessage.replace(value, "[REDACTED]");
            }
            System.out.println("{\"success\":false,\"host\":" + quote(host) + ",\"sql_state\":" + quote(ex.getSQLState())
                + ",\"message\":" + quote(safeMessage) + ",\"error_types\":[" + String.join(",", causes) + "],\"error_reasons\":[" + String.join(",", reasons) + "],\"elapsed_ms\":" + ((System.nanoTime()-began)/1000000) + "}");
            System.exit(2);
            return;
        }
        System.out.println(successReport);
    }
}
