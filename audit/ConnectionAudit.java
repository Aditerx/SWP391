import java.nio.file.*;
import java.sql.*;
import java.util.*;
public class ConnectionAudit {
  public static void main(String[] args) throws Exception {
    Properties p = new Properties();
    for (String line : Files.readAllLines(Path.of(".env"))) {
      int eq=line.indexOf('='); if(eq>0 && !line.stripLeading().startsWith("#")) p.setProperty(line.substring(0,eq).trim(),line.substring(eq+1).trim());
    }
    try (Connection c=DriverManager.getConnection(p.getProperty("DB_URL"),p.getProperty("DB_USERNAME"),p.getProperty("DB_PASSWORD"))) {
      c.setReadOnly(true);
      try (Statement s=c.createStatement()) {
        s.setQueryTimeout(15);
        try (ResultSet r=s.executeQuery("SELECT pid,state,wait_event_type,wait_event,pg_blocking_pids(pid)::text FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() AND backend_type='client backend'")) {
          while(r.next()) System.out.println(r.getInt(1)+" | "+r.getString(2)+" | "+r.getString(3)+" | "+r.getString(4)+" | blockers="+r.getString(5));
        }
      }
    }
  }
}
