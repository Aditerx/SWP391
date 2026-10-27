package com.sportscenter.communication;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Real SMTP transport sink: loopback only, no forwarding and no external mailbox. */
final class LoopbackSmtpCapture implements AutoCloseable {
    private final ServerSocket server;
    private final ExecutorService workers = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "proof-smtp"); t.setDaemon(true); return t;
    });
    private final List<MimeMessage> received = new CopyOnWriteArrayList<>();
    private final Queue<String> errors = new ConcurrentLinkedQueue<>();

    LoopbackSmtpCapture() throws IOException {
        server = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
        workers.submit(() -> {
            while (!server.isClosed()) {
                try { Socket socket = server.accept(); workers.submit(() -> accept(socket)); }
                catch (IOException ex) { if (!server.isClosed()) throw new UncheckedIOException(ex); }
            }
        });
    }
    int port() { return server.getLocalPort(); }
    int count(String recipient) {
        return (int) received.stream().filter(m -> addressedTo(m, recipient)).count();
    }
    String awaitText(String recipient, int ordinal) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            List<MimeMessage> matches = received.stream().filter(m -> addressedTo(m, recipient)).toList();
            if (matches.size() >= ordinal) return String.valueOf(matches.get(ordinal - 1).getContent());
            Thread.sleep(25);
        }
        throw new AssertionError("Expected local SMTP delivery was not captured within 10 seconds; captured=" + received.size() + "; protocolErrors=" + errors);
    }
    private boolean addressedTo(MimeMessage message, String recipient) {
        try { return Arrays.stream(message.getAllRecipients()).anyMatch(a -> a.toString().equals(recipient)); }
        catch (Exception ex) { throw new IllegalStateException("Cannot inspect captured test message", ex); }
    }
    private void accept(Socket socket) {
        try (socket;
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            socket.setSoTimeout(10000);
            reply(out, "220 localhost proof SMTP");
            String line;
            while ((line = in.readLine()) != null) {
                String command = line.toUpperCase(Locale.ROOT);
                if (command.startsWith("EHLO") || command.startsWith("HELO")) reply(out, "250 localhost");
                else if (command.startsWith("MAIL FROM:") || command.startsWith("RCPT TO:") || command.equals("RSET") || command.equals("NOOP")) reply(out, "250 OK");
                else if (command.equals("DATA")) {
                    reply(out, "354 End with dot");
                    StringBuilder data = new StringBuilder();
                    while ((line = in.readLine()) != null && !line.equals(".")) {
                        data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
                    }
                    received.add(new MimeMessage(Session.getInstance(new Properties()),
                            new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.UTF_8))));
                    reply(out, "250 Accepted into local test mailbox");
                } else if (command.equals("QUIT")) { reply(out, "221 Bye"); break; }
                else reply(out, "502 Unsupported test SMTP command");
            }
        } catch (Exception ex) { errors.add(ex.getClass().getSimpleName()); throw new IllegalStateException("Local SMTP capture failed", ex); }
    }
    private void reply(PrintWriter out, String line) { out.print(line + "\r\n"); out.flush(); }
    @Override public void close() throws IOException { server.close(); workers.shutdownNow(); }
}
