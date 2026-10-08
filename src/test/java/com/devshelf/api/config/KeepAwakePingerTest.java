package com.devshelf.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeepAwakePingerTest {

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/actuator/health", exchange -> {
            hits.incrementAndGet();
            byte[] body = "{\"status\":\"UP\"}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void pingReachesTheTargetOnceAndReturnsItsStatus() {
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/actuator/health";

        int status = new KeepAwakePinger(url).ping();

        assertThat(status).isEqualTo(200);
        assertThat(hits).hasValue(1);
    }

    @Test
    void aTargetThatIsDownIsReportedNotThrown() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        int status = new KeepAwakePinger("http://127.0.0.1:" + closedPort + "/actuator/health").ping();

        assertThat(status).isEqualTo(-1);
    }
}
