package com.sportscenter.invoice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional one-shot runtime diagnostics. Fixed public targets; no merchant, key, invoice or payload. */
@Component @Slf4j
@ConditionalOnProperty(name="vnpay.network-probe-enabled",havingValue="true")
public class VnPayNetworkProbe {
    static final List<String> TARGETS=List.of("sandbox.vnpayment.vn","example.com");
    interface Checks {
        String dns(String host) throws Exception;
        void tcp(String host) throws Exception;
        int head(String host,int connectSeconds) throws Exception;
    }
    private final Checks checks;
    private final AtomicBoolean started=new AtomicBoolean();
    public VnPayNetworkProbe(){this(new SystemChecks());}
    VnPayNetworkProbe(Checks checks){this.checks=checks;}

    @EventListener(ApplicationReadyEvent.class)
    public void onReady(){
        if(!started.compareAndSet(false,true))return;
        var worker=new Thread(this::runProbe,"vnpay-network-probe");
        worker.setDaemon(true);worker.start();
    }

    void runProbe(){
        try {
            for(String host:TARGETS){
                long start=System.nanoTime();
                try {log.info("VNPAY_NET host={} phase=DNS result=OK ips={} elapsedMs={}",host,checks.dns(host),elapsed(start));}
                catch(Exception ex){logFailure(host,"DNS",start,ex);if(interrupted(ex))return;continue;}
                start=System.nanoTime();
                try{checks.tcp(host);log.info("VNPAY_NET host={} phase=TCP_15S result=OK elapsedMs={}",host,elapsed(start));}
                catch(Exception ex){logFailure(host,"TCP_15S",start,ex);if(interrupted(ex))return;continue;}
                for(int seconds:List.of(3,15)){
                    start=System.nanoTime();
                    try {int status=checks.head(host,seconds);log.info("VNPAY_NET host={} phase=HTTPS_{}S result=HTTP_{} elapsedMs={}",host,seconds,status,elapsed(start));}
                    catch(Exception ex){logFailure(host,"HTTPS_"+seconds+"S",start,ex);if(interrupted(ex))return;}
                }
            }
        } finally {log.info("VNPAY_NET finished=true");}
    }
    private static boolean interrupted(Exception ex){
        if(ex instanceof InterruptedException){Thread.currentThread().interrupt();return true;}
        return false;
    }
    private static long elapsed(long start){return (System.nanoTime()-start)/1_000_000;}
    private static void logFailure(String host,String phase,long start,Exception ex){
        // Class name is safe; exception messages may contain arbitrary external data.
        log.info("VNPAY_NET host={} phase={} result={} elapsedMs={}",host,phase,ex.getClass().getSimpleName(),elapsed(start));
    }
    private static class SystemChecks implements Checks {
        public String dns(String host)throws Exception{
            return String.join(",",Arrays.stream(InetAddress.getAllByName(host)).map(InetAddress::getHostAddress).toList());
        }
        public void tcp(String host)throws Exception{
            try(var socket=new Socket()){socket.connect(new InetSocketAddress(host,443),15_000);}
        }
        public int head(String host,int connectSeconds)throws Exception{
            // HTTPS uses the JVM's normal trust and hostname verification. No redirects or request body.
            URI uri=URI.create("https://"+host+(host.equals("sandbox.vnpayment.vn")?"/merchant_webapi/api/transaction":"/"));
            var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectSeconds))
                    .followRedirects(HttpClient.Redirect.NEVER).build();
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(connectSeconds+5))
                    .method("HEAD",HttpRequest.BodyPublishers.noBody()).build();
            return client.send(request,HttpResponse.BodyHandlers.discarding()).statusCode();
        }
    }
}
