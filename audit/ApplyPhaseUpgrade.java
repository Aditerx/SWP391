import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.net.URI;
import java.security.MessageDigest;

/** Exact reviewed existing-schema upgrade only. Local proof never reads .env. */
public class ApplyPhaseUpgrade {
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    public static void main(String[] args){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));Connection connection=null;
        try{
            if(args.length!=3)throw new IllegalArgumentException("Bundle path, approved hash and explicit target confirmation required");
            byte[] bundle=Files.readAllBytes(Path.of(args[0]));
            if(!hash(bundle).equals(args[1]))throw new IllegalArgumentException("Bundle hash mismatch");
            Properties env=new Properties();String url;
            if(args[2].startsWith("--local-proof=")){
                url=args[2].substring("--local-proof=".length());
                if(!url.matches("jdbc:postgresql://127\\.0\\.0\\.1:55440/scms_phases_upgrade_[a-z0-9_]+"))throw new IllegalArgumentException("Guarded disposable local target required");
                env.setProperty("DB_USERNAME","postgres");env.setProperty("DB_PASSWORD","scms-local-only");
            }else{
                if(!args[2].startsWith("--confirm-target="))throw new IllegalArgumentException("Explicit target fingerprint required");
                for(String line:Files.readAllLines(Path.of(".env"))){int at=line.indexOf('=');if(at>0&&!line.stripLeading().startsWith("#"))env.setProperty(line.substring(0,at).trim(),line.substring(at+1).trim());}
                url=env.getProperty("DB_URL","");
                if(!url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException("Configured PostgreSQL JDBC required");
            }
            URI target=URI.create(url.substring(5));
            if(target.getHost()==null||target.getUserInfo()!=null)throw new IllegalArgumentException("Separate credentials required");
            if(target.getRawQuery()!=null)for(String part:target.getRawQuery().split("&")){
                String[] pair=part.split("=",2);
                if(java.net.URLDecoder.decode(pair[0],java.nio.charset.StandardCharsets.UTF_8).equals("sslmode")){
                    String mode=pair.length>1?java.net.URLDecoder.decode(pair[1],java.nio.charset.StandardCharsets.UTF_8):"";
                    if(!Set.of("require","verify-ca","verify-full").contains(mode))throw new IllegalArgumentException("TLS required");
                }
            }
            String fingerprint=hash((target.getHost()+":"+target.getPort()+"/"+target.getPath()+"|"+env.getProperty("DB_USERNAME")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(args[2].startsWith("--confirm-target=")&&!args[2].substring("--confirm-target=".length()).equals(fingerprint))throw new IllegalArgumentException("Configured target changed");
            Properties jdbc=new Properties();jdbc.setProperty("user",env.getProperty("DB_USERNAME"));jdbc.setProperty("password",env.getProperty("DB_PASSWORD"));
            jdbc.setProperty("connectTimeout","10");jdbc.setProperty("socketTimeout","60");
            if(!args[2].startsWith("--local-proof="))jdbc.setProperty("sslmode","require");
            DriverManager.setLoginTimeout(10);
            connection=DriverManager.getConnection(url,jdbc);connection.setAutoCommit(false);connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try(Statement s=connection.createStatement()){
                s.setQueryTimeout(30);
                String sql=new String(bundle,java.nio.charset.StandardCharsets.UTF_8).replaceAll("(?m)^(?:BEGIN;|COMMIT;)\\s*$","");
                s.execute(sql);
                String data;
                try(ResultSet r=s.executeQuery("SELECT json_build_object('subjects',(SELECT count(*) FROM subjects),'users',(SELECT count(*) FROM users),'invoices',(SELECT count(*) FROM invoices),'subscriptions',(SELECT count(*) FROM member_packages),'sessions',(SELECT count(*) FROM sessions),'enrollments',(SELECT count(*) FROM class_enrollments))::text")){r.next();data=r.getString(1);}
                connection.commit();
                System.out.println("{\"committed\":true,\"targetFingerprint\":\""+fingerprint+"\",\"bundleSha256\":\""+hash(bundle)+"\",\"originalRowsPreserved\":true,\"counts\":"+data+"}");
            }
        }catch(Exception error){boolean rolledBack=false;try{if(connection!=null){connection.rollback();rolledBack=true;}}catch(Exception ignored){}
            String state=error instanceof SQLException sql?sql.getSQLState():"";
            System.out.println("{\"committed\":false,\"rolledBack\":"+rolledBack+",\"errorType\":\""+error.getClass().getSimpleName()+"\",\"sqlState\":\""+state+"\",\"credentialsOmitted\":true}");System.exit(2);
        }finally{try{if(connection!=null)connection.close();}catch(Exception ignored){}}
    }
}
