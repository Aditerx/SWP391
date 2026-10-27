import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.security.MessageDigest;
import java.net.URI;

/** Production metadata only: no Spring, jobs, DDL, business rows, or credential output. */
public class PhaseReleasePreflight {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        try {
            Properties env=new Properties();
            for(String line:Files.readAllLines(Path.of(".env"))){int at=line.indexOf('=');if(at>0&&!line.stripLeading().startsWith("#"))env.setProperty(line.substring(0,at).trim(),line.substring(at+1).trim());}
            String url=env.getProperty("DB_URL","");
            if(!url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException("Configured PostgreSQL JDBC expected");
            URI target=URI.create(url.substring(5));
            // Enforce TLS in the client without rewriting the owner's .env URL.
            if(target.getRawQuery()!=null)for(String part:target.getRawQuery().split("&")){
                String[] pair=part.split("=",2);
                if(java.net.URLDecoder.decode(pair[0],java.nio.charset.StandardCharsets.UTF_8).equals("sslmode")){
                    String mode=pair.length>1?java.net.URLDecoder.decode(pair[1],java.nio.charset.StandardCharsets.UTF_8):"";
                    if(!Set.of("require","verify-ca","verify-full").contains(mode))throw new IllegalArgumentException("Read-only checker requires TLS");
                }
            }
            if(target.getUserInfo()!=null)throw new IllegalArgumentException("Embedded credentials are unsupported");
            String fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((target.getHost()+":"+target.getPort()+"/"+target.getPath()+"|"+env.getProperty("DB_USERNAME")).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            Properties jdbc=new Properties();jdbc.setProperty("sslmode","require");jdbc.setProperty("user",env.getProperty("DB_USERNAME"));jdbc.setProperty("password",env.getProperty("DB_PASSWORD"));jdbc.setProperty("connectTimeout","10");jdbc.setProperty("socketTimeout","15");
            DriverManager.setLoginTimeout(10);
            try(Connection c=DriverManager.getConnection(url,jdbc)){
                c.setReadOnly(true);c.setAutoCommit(false);
                try(Statement s=c.createStatement()){
                    s.setQueryTimeout(15);
                    int tables=0;try(ResultSet r=s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")){r.next();tables=r.getInt(1);}
                    String ro="";try(ResultSet r=s.executeQuery("SHOW transaction_read_only")){r.next();ro=r.getString(1);}
                    List<String> missing=new ArrayList<>();
                    for(String item:List.of("member_packages.subject_id","member_packages.expires_at","member_packages.purchase_amount","sessions.booking_key","sessions.center_invalidated","holidays.actual_closed_until","invoices.cash_confirmation_reversed","class_enrollments.eligibility_subscription_id")){
                        String[] p=item.split("\\.");
                        try(PreparedStatement ps=c.prepareStatement("SELECT count(*) FROM information_schema.columns WHERE table_schema='public' AND table_name=? AND column_name=?")){ps.setString(1,p[0]);ps.setString(2,p[1]);try(ResultSet r=ps.executeQuery()){r.next();if(r.getInt(1)==0)missing.add(item);}}
                    }
                    c.rollback();
                    System.out.println("{\"targetFingerprint\":\""+fingerprint+"\",\"readOnly\":\""+ro+"\",\"publicTableCount\":"+tables+",\"missingRequiredColumns\":["+missing.stream().map(x->"\""+x+"\"").collect(java.util.stream.Collectors.joining(","))+"],\"rolledBack\":true}");
                }finally{if(!c.isClosed())c.rollback();}
            }
        }catch(Exception error){String state=error instanceof SQLException sql?sql.getSQLState():"";System.out.println("{\"errorType\":\""+error.getClass().getSimpleName()+"\",\"sqlState\":\""+state+"\",\"credentialsOmitted\":true}");System.exit(2);}
    }
}
