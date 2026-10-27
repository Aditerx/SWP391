import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.net.URI;
import java.security.MessageDigest;

/** Explicit empty-schema release executor. Never boots the application or creates demonstration transactions. */
public class ApplyPhaseRelease {
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));Connection connection=null;
        try{
            if(args.length!=3)throw new IllegalArgumentException("bundle path, expected bundle hash, and --local-proof=URL or --confirm-target=hash required");
            byte[] bytes=Files.readAllBytes(Path.of(args[0]));if(!hash(bytes).equals(args[1]))throw new IllegalArgumentException("Release bundle hash mismatch");
            Properties env=new Properties();String url;
            if(args[2].startsWith("--local-proof=")){
                url=args[2].substring("--local-proof=".length());
                if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_[a-z0-9_]+(?:\\?sslmode=disable)?"))throw new IllegalArgumentException("Disposable local target required");
                env.setProperty("DB_USERNAME","postgres");env.setProperty("DB_PASSWORD","scms-local-only");
            }else{
                if(!args[2].startsWith("--confirm-target="))throw new IllegalArgumentException("Explicit target confirmation required");
                for(String line:Files.readAllLines(Path.of(".env"))){int at=line.indexOf('=');if(at>0&&!line.stripLeading().startsWith("#"))env.setProperty(line.substring(0,at).trim(),line.substring(at+1).trim());}
                url=env.getProperty("DB_URL","");if(!url.startsWith("jdbc:postgresql://")||!url.contains("sslmode=require"))throw new IllegalArgumentException("Production requires configured TLS");
            }
            URI target=URI.create(url.substring(5));if(target.getUserInfo()!=null)throw new IllegalArgumentException("Embedded credentials are unsupported");
            String fingerprint=hash((target.getHost()+":"+target.getPort()+"/"+target.getPath()+"|"+env.getProperty("DB_USERNAME")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(args[2].startsWith("--confirm-target=")&&!args[2].substring("--confirm-target=".length()).equals(fingerprint))throw new IllegalArgumentException("Target changed since preflight");
            Properties jdbc=new Properties();jdbc.setProperty("user",env.getProperty("DB_USERNAME"));jdbc.setProperty("password",env.getProperty("DB_PASSWORD"));jdbc.setProperty("connectTimeout","10");jdbc.setProperty("socketTimeout","30");
            connection=DriverManager.getConnection(url,jdbc);connection.setAutoCommit(false);connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try(Statement s=connection.createStatement()){
                s.setQueryTimeout(30);s.execute("SET LOCAL lock_timeout='5s'");
                String body=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).replaceAll("(?m)^(?:BEGIN;|COMMIT;)\\s*$","");
                s.execute(body);
                for(String table:List.of("users","invoices","member_packages","sessions","class_enrollments")){
                    try(ResultSet r=s.executeQuery("SELECT count(*) FROM "+table)){r.next();if(r.getInt(1)!=0)throw new IllegalStateException("Unexpected business rows before release commit");}
                }
                try(ResultSet r=s.executeQuery("SELECT count(*) FROM subjects WHERE normal_price>0 AND advanced_price>0")){r.next();if(r.getInt(1)!=9)throw new IllegalStateException("Catalog count mismatch");}
                connection.commit();
                System.out.println("{\"committed\":true,\"targetFingerprint\":\""+fingerprint+"\",\"bundleSha256\":\""+hash(bytes)+"\",\"subjects\":9,\"businessRowsCreated\":0,\"accountsCreated\":0}");
            }
        }catch(Exception error){try{if(connection!=null)connection.rollback();}catch(Exception ignored){}String state=error instanceof SQLException sql?sql.getSQLState():"";System.out.println("{\"committed\":false,\"errorType\":\""+error.getClass().getSimpleName()+"\",\"sqlState\":\""+state+"\",\"credentialsOmitted\":true}");System.exit(2);}
        finally{try{if(connection!=null)connection.close();}catch(Exception ignored){}}
    }
}
