package com.sportscenter.invoice;

import org.junit.jupiter.api.Test;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.junit.jupiter.api.Assertions.*;

class VnPayNetworkProbeTest {
    @Test void probeBeanIsDisabledByDefaultAndCanBeExplicitlyEnabled(){
        var runner=new ApplicationContextRunner().withUserConfiguration(VnPayNetworkProbe.class);
        runner.run(context->assertTrue(context.getBeansOfType(VnPayNetworkProbe.class).isEmpty()));
        // A plain context refresh emits no ApplicationReadyEvent, so this performs no network I/O.
        runner.withPropertyValues("vnpay.network-probe-enabled=true").run(context->{
            assertNull(context.getStartupFailure());assertEquals(1,context.getBeansOfType(VnPayNetworkProbe.class).size());
        });
    }
    static class FakeChecks implements VnPayNetworkProbe.Checks {
        final List<String> calls=Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch heads=new CountDownLatch(4);
        String failPhase;
        public String dns(String host)throws Exception{
            calls.add("DNS "+host);
            if(host.equals("sandbox.vnpayment.vn")&&"DNS".equals(failPhase))throw new UnknownHostException("sensitive-data");
            return "192.0.2.1";
        }
        public void tcp(String host)throws Exception{
            calls.add("TCP "+host);
            if(host.equals("sandbox.vnpayment.vn")&&"TCP".equals(failPhase))throw new SocketTimeoutException("sensitive-data");
        }
        public int head(String host,int seconds)throws Exception{
            calls.add("HEAD "+host+" "+seconds);heads.countDown();
            if(host.equals("sandbox.vnpayment.vn")&&seconds==3&&"HTTPS".equals(failPhase))throw new java.net.http.HttpConnectTimeoutException("sensitive-data");
            return 405;
        }
    }
    @Test void startupEventRunsOnceWithoutBlockingCaller()throws Exception{
        var c=new FakeChecks();var probe=new VnPayNetworkProbe(c);
        probe.onReady();probe.onReady();assertTrue(c.heads.await(2,TimeUnit.SECONDS));
        assertEquals(2,c.calls.stream().filter(s->s.startsWith("DNS")).count());
        assertEquals(4,c.calls.stream().filter(s->s.startsWith("HEAD")).count());
    }
    @Test void failedTcpSkipsTlsForThatTargetAndStillChecksControl(){
        var c=new FakeChecks();c.failPhase="TCP";new VnPayNetworkProbe(c).runProbe();
        assertFalse(c.calls.stream().anyMatch(s->s.startsWith("HEAD sandbox")));
        assertTrue(c.calls.contains("HEAD example.com 15"));
    }
    @Test void failedDnsSkipsTargetAndStillChecksControl(){
        var c=new FakeChecks();c.failPhase="DNS";new VnPayNetworkProbe(c).runProbe();
        assertFalse(c.calls.contains("TCP sandbox.vnpayment.vn"));
        assertTrue(c.calls.contains("HEAD example.com 15"));
    }
    @Test void shortHttpsTimeoutStillTestsLongerHandshakeAndControl(){
        var c=new FakeChecks();c.failPhase="HTTPS";new VnPayNetworkProbe(c).runProbe();
        assertTrue(c.calls.contains("HEAD sandbox.vnpayment.vn 15"));
        assertTrue(c.calls.contains("HEAD example.com 15"));
    }
    @Test void exceptionMessagesNeverAppearInDiagnosticLog(){
        var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(VnPayNetworkProbe.class);
        var previous=logger.getLevel();logger.setLevel(ch.qos.logback.classic.Level.INFO);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();logger.addAppender(appender);
        try{
            var c=new FakeChecks();c.failPhase="TCP";new VnPayNetworkProbe(c).runProbe();
            String messages=String.join("\n",appender.list.stream().map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage).toList());
            assertTrue(messages.contains("SocketTimeoutException"));assertFalse(messages.contains("sensitive-data"));
        }finally{logger.detachAppender(appender);appender.stop();logger.setLevel(previous);}
    }
}
