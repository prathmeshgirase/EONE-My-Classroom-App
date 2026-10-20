package com.eone;

import java.nio.ByteBuffer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;

/**
 * Temporary, credential-free production connectivity probe.
 * Does not change JDBC settings or send a PostgreSQL startup/authentication packet.
 */
public final class DatabaseTlsDiagnostic {
    private DatabaseTlsDiagnostic() {}

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        if (!Arrays.asList(System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "")
                .split(",")).contains("production")) return;
        String phase = "configuration";
        try {
            String url = System.getenv().getOrDefault("DATABASE_URL", "");
            if (!url.startsWith("jdbc:postgresql://")) {
                report("configuration", "EXPECTED_JDBC_POSTGRESQL_URL");
                return;
            }
            URI endpoint = URI.create(url.substring("jdbc:".length()));
            String host = endpoint.getHost();
            int port = endpoint.getPort() == -1 ? 5432 : endpoint.getPort();
            if (host == null || endpoint.getUserInfo() != null) {
                report("configuration", "INVALID_HOST_OR_EMBEDDED_CREDENTIALS");
                return;
            }
            phase = "dns";
            InetAddress[] addresses = InetAddress.getAllByName(host);
            report(phase, "OK addressCount=" + addresses.length);
            for (InetAddress address : addresses) {
                phase = "tcp";
                String family = address.getAddress().length == 4 ? "IPv4" : "IPv6";
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(address, port), 5000);
                    socket.setSoTimeout(5000);
                    report(phase, "OK " + family);
                    phase = "postgres_ssl_request";
                    byte[] request = ByteBuffer.allocate(8).putInt(8).putInt(80877103).array();
                    socket.getOutputStream().write(request);
                    socket.getOutputStream().flush();
                    int response = socket.getInputStream().read();
                    if (response != 'S') {
                        report(phase, "UNEXPECTED_RESPONSE_BYTE=" + response + " " + family);
                        continue;
                    }
                    report(phase, "SSL_ACCEPTED " + family);
                    phase = "tls";
                    try (SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                            .createSocket(socket, host, port, true)) {
                        SSLParameters parameters = tls.getSSLParameters();
                        parameters.setServerNames(List.of(new SNIHostName(host)));
                        parameters.setEndpointIdentificationAlgorithm("HTTPS");
                        tls.setSSLParameters(parameters);
                        tls.setSoTimeout(5000);
                        tls.startHandshake();
                        report(phase, "VERIFIED_OK " + family + " " + tls.getSession().getProtocol());
                    }
                } catch (Exception failure) {
                    reportFailure(phase, failure);
                }
            }
        } catch (Exception failure) {
            reportFailure(phase, failure);
        }
        report("complete", "NO_DATABASE_CREDENTIALS_SENT; NORMAL_APPLICATION_STARTUP_CONTINUES");
    }

    private static void reportFailure(String phase, Throwable failure) {
        // Exception messages can contain connection strings; print class names only.
        for (int depth = 0; failure != null && depth < 5; depth++, failure = failure.getCause()) {
            report(phase, "FAIL cause=" + failure.getClass().getSimpleName());
        }
    }

    private static void report(String phase, String result) {
        System.out.println("[EONE-TLS-DIAG] " + phase + " " + result);
    }
}
