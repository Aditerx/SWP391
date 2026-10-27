import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.net.URI;
import java.security.MessageDigest;

/** Read-only comparison with the public Render catalog. No Spring, jobs or credentials in output. */
public class CheckRenderTarget {
    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        Properties env=new Properties();
        for(String line:Files.readAllLines(Path.of(".env"))){int at=line.indexOf('=');if(at>0&&!line.stripLeading().startsWith("#"))env.setProperty(line.substring(0,at).trim(),line.substring(at+1).trim());}
        String url=env.getProperty("DB_URL","");
        if(!url.startsWith("jdbc:postgresql://")||!url.contains("sslmode=require"))throw new IllegalArgumentException("Configured PostgreSQL TLS required");
        URI target=URI.create(url.substring(5));if(target.getUserInfo()!=null)throw new IllegalArgumentException("Embedded credentials unsupported");
        String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((target.getHost()+":"+target.getPort()+"/"+target.getPath()+"|"+env.getProperty("DB_USERNAME")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Properties jdbc=new Properties();jdbc.setProperty("user",env.getProperty("DB_USERNAME"));jdbc.setProperty("password",env.getProperty("DB_PASSWORD"));jdbc.setProperty("connectTimeout","10");jdbc.setProperty("socketTimeout","15");
        try(Connection c=DriverManager.getConnection(url,jdbc)){
            c.setReadOnly(true);c.setAutoCommit(false);
            try(Statement s=c.createStatement()){
                s.setQueryTimeout(15);
                String query="SELECT json_build_object('readOnly',current_setting('transaction_read_only'),'subjects',(SELECT json_agg(json_build_object('id',subject_id,'slug',slug) ORDER BY subject_id) FROM subjects WHERE status='Active'),'users',(SELECT count(*) FROM users),'invoices',(SELECT count(*) FROM invoices),'subscriptions',(SELECT count(*) FROM member_packages),'sessions',(SELECT count(*) FROM sessions))::text";
                String data;
                try(ResultSet r=s.executeQuery(query)){r.next();data=r.getString(1);}
                c.rollback();
                System.out.println("{\"targetFingerprint\":\""+fingerprint+"\",\"database\":"+data+",\"rolledBack\":true}");
            }
        }catch(SQLException e){System.out.println("{\"readOnlyCheckFailed\":true,\"sqlState\":\""+e.getSQLState()+"\",\"credentialsOmitted\":true}");System.exit(2);}
    }
}
