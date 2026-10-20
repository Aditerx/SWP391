import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Read-only inspection; never prints credentials or member records. */
public class SchemaAudit {
  public static void main(String[] args) throws Exception {
    Properties p = new Properties();
    for (String line : Files.readAllLines(Path.of(".env"))) {
      int eq = line.indexOf('=');
      if (eq > 0 && !line.stripLeading().startsWith("#")) p.setProperty(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
    }
    try (Connection c = DriverManager.getConnection(p.getProperty("DB_URL"), p.getProperty("DB_USERNAME"), p.getProperty("DB_PASSWORD"))) {
      c.setReadOnly(true);
      c.setAutoCommit(false);
      try (Statement s = c.createStatement()) {
        s.setQueryTimeout(25);
        String[] queries = {
          "SELECT current_database(),current_schema(),version()",
          "SELECT table_name,column_name,data_type,column_default,is_nullable FROM information_schema.columns WHERE table_schema='public' AND table_name IN ('classes','invoices','class_feedbacks','member_packages','class_enrollments','users','subjects','coaches','media_assets') ORDER BY table_name,ordinal_position",
          "SELECT t.relname,c.conname,pg_get_constraintdef(c.oid) FROM pg_constraint c JOIN pg_class t ON t.oid=c.conrelid JOIN pg_namespace n ON n.oid=t.relnamespace WHERE n.nspname='public' AND t.relname IN ('classes','invoices','class_feedbacks','member_packages','class_enrollments','users') ORDER BY t.relname,c.conname",
          "SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND tablename IN ('invoices','class_feedbacks','class_enrollments') ORDER BY tablename,indexname",
          "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema='public' ORDER BY sequence_name",
          "SELECT r.role_name,p.permission_name FROM roles r JOIN role_permissions rp ON rp.role_id=r.role_id JOIN permissions p ON p.permission_id=rp.permission_id ORDER BY r.role_name,p.permission_name",
          "SELECT event_object_table,trigger_name FROM information_schema.triggers WHERE trigger_schema='public' ORDER BY event_object_table,trigger_name",
          "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name",
          "SELECT payment_status,payment_method,count(*),count(*) FILTER(WHERE subscription_id IS NOT NULL),count(*) FILTER(WHERE class_id IS NOT NULL),count(*) FILTER(WHERE expires_at IS NOT NULL) FROM invoices GROUP BY payment_status,payment_method ORDER BY payment_status,payment_method",
          "SELECT status,count(*),count(*) FILTER(WHERE start_date<=CURRENT_DATE AND end_date>=CURRENT_DATE) FROM member_packages GROUP BY status ORDER BY status",
          "SELECT status,count(*),count(*) FILTER(WHERE tuition_fee>0) FROM classes GROUP BY status ORDER BY status",
          "SELECT status,count(*),count(*) FILTER(WHERE session_date>=CURRENT_DATE) FROM sessions GROUP BY status ORDER BY status"
        };
        for (String q : queries) {
          System.out.println("QUERY: " + q);
          try (ResultSet rs = s.executeQuery(q)) {
            int n = rs.getMetaData().getColumnCount();
            while (rs.next()) { for (int i=1;i<=n;i++) System.out.print((i>1?" | ":"")+rs.getString(i)); System.out.println(); }
          }
        }
      } finally { c.rollback(); }
    }
  }
}
