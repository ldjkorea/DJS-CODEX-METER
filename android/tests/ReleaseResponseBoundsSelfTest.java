package dev.bennett.codexmeter;

import android.content.Context;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real release reader/preferences, in-memory Android substitutes, NO sockets/devices. */
public final class ReleaseResponseBoundsSelfTest {
    static int checks, status = 200;
    static byte[] response;
    static Connection connection;
    static void check(boolean value, String label) {
        checks++;
        if (!value) throw new AssertionError(label);
    }
    static final class Tracked extends ByteArrayInputStream {
        boolean closed;
        Tracked(byte[] bytes) { super(bytes); }
        @Override public void close() { closed = true; }
        @Override public synchronized int read(byte[] bytes, int offset, int length) {
            return super.read(bytes, offset, Math.min(4093, length));
        }
    }
    static final class Connection extends HttpURLConnection {
        final Tracked input;
        boolean disconnected;
        Connection(URL url) { super(url); input = new Tracked(response); }
        public void connect() { }
        public boolean usingProxy() { return false; }
        public void disconnect() { disconnected = true; }
        public int getResponseCode() { return status; }
        public String getHeaderField(String key) { return "ETag".equals(key) ? "synthetic-new-etag" : null; }
        public InputStream getInputStream() { return input; }
        public InputStream getErrorStream() { return input; }
    }
    static String padded(int bytes, boolean unicode) {
        String suffix = unicode ? "[\"한\"]" : "[]";
        return " ".repeat(bytes - suffix.getBytes(StandardCharsets.UTF_8).length) + suffix;
    }
    static String read(Method reader, Tracked input, int max) throws Exception {
        try { return (String) reader.invoke(null, input, max); }
        catch (InvocationTargetException e) { throw (Exception) e.getCause(); }
    }
    public static void main(String[] args) throws Exception {
        int max = Integer.parseInt(args[0]);
        Field field = ReleaseUpdateClient.class.getDeclaredField("MAX_RESPONSE_BYTES");
        field.setAccessible(true);
        check(field.getInt(null) == max, "Actual pinned source response bound");
        Method reader = ReleaseUpdateClient.class.getDeclaredMethod("read", InputStream.class, int.class);
        reader.setAccessible(true);
        check(read(reader, null, max).isEmpty(), "Null response handled");
        for (boolean unicode : new boolean[]{false, true}) {
            for (int size : new int[]{max - 1, max, max + 1}) {
                String body = padded(size, unicode);
                Tracked input = new Tracked(body.getBytes(StandardCharsets.UTF_8));
                try {
                    String result = read(reader, input, max);
                    check(size <= max && result.equals(body), "Exact byte boundary accepts intact JSON");
                } catch (IllegalStateException expected) {
                    check(size > max && expected.getMessage().equals("GitHub returned too much release metadata."),
                            "Oversize rejected without payload disclosure");
                }
                check(input.closed, "Reader closes stream on success/failure");
            }
        }
        // Every https URL is intercepted in this JVM. No server or network is used.
        URL.setURLStreamHandlerFactory(protocol -> {
            if (!"https".equals(protocol)) throw new AssertionError("Unexpected network protocol");
            return new URLStreamHandler() {
                protected URLConnection openConnection(URL url) { return connection = new Connection(url); }
            };
        });
        Context context = new Context();
        String prior = "[ {\"tag_name\":\"v3.1.1\",\"html_url\":\"https://github.com/ldjkorea/codex-meter-ko/releases/tag/v3.1.1\",\"assets\":[{\"name\":\"CodexMeter-3.1.1-ko.apk\",\"size\":100,\"browser_download_url\":\"https://github.com/ldjkorea/codex-meter-ko/releases/download/v3.1.1/CodexMeter-3.1.1-ko.apk\"},{\"name\":\"SHA256SUMS.txt\",\"browser_download_url\":\"https://github.com/ldjkorea/codex-meter-ko/releases/download/v3.1.1/SHA256SUMS.txt\"}]} ]";
        for (int size : new int[]{max - 1, max, max + 1}) {
            UpdatePreferences.saveSuccess(context, prior, "synthetic-prior-etag");
            UpdatePreferences.setAutomaticChecks(context, false);
            response = padded(size, false).getBytes(StandardCharsets.UTF_8);
            try {
                List<GitHubRelease> result = ReleaseUpdateClient.check(context);
                check(size <= max && result.isEmpty(), "Actual request path accepts bounded JSON");
                check(UpdatePreferences.etag(context).equals("synthetic-new-etag"), "Success commits new ETag");
                check(UpdatePreferences.lastError(context).isEmpty(), "Successful retry clears prior error");
            } catch (IllegalStateException expected) {
                check(size > max, "Actual request fails only for oversize");
                check(UpdatePreferences.releases(context).size() == 1, "Existing parsed release retained");
                check(context.getSharedPreferences("codex_meter_updates_v1", 0).getString("releases", "").equals(prior),
                        "Exact cached JSON retained after failed read");
                check(UpdatePreferences.etag(context).equals("synthetic-prior-etag"), "Prior ETag retained");
                check(UpdatePreferences.lastError(context).equals(expected.getMessage()), "Bounded safe error recorded");
            }
            check(!UpdatePreferences.automaticChecks(context), "User check preference retained");
            check(connection.disconnected && connection.input.closed, "Request cleans connection and body");
        }
        // The last oversized request must recover on a subsequent successful request.
        response = "[]".getBytes(StandardCharsets.UTF_8);
        check(ReleaseUpdateClient.check(context).isEmpty() && UpdatePreferences.lastError(context).isEmpty(),
                "Retry after oversized response succeeds");
        status = 304;
        check(ReleaseUpdateClient.check(context).isEmpty(), "Not-modified uses valid cached JSON");
        check(connection.disconnected, "Not-modified disconnects");
        System.out.println("Release response bound " + max + ": " + checks + " checks passed; actual reader/preferences; synthetic/no sockets.");
    }
}
