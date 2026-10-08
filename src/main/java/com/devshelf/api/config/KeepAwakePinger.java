package com.devshelf.api.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the demo awake on Render's free plan, which stops an instance after ~15 minutes without an inbound
 * request and then needs minutes to start it again. Every {@code app.keep-awake-interval} this bean GETs
 * {@code app.keep-awake-url} - the service's own public health URL - which counts as inbound traffic.
 * Nothing is created when the URL is not set (local runs, tests). The GitHub Actions ping in
 * {@code .github/workflows/keep-warm.yml} does the same from outside; GitHub's schedule can run late, so both exist.
 */
@Component
@EnableScheduling
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${app.keep-awake-url:}')")
public class KeepAwakePinger {

    private static final Logger log = LoggerFactory.getLogger(KeepAwakePinger.class);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final URI target;

    public KeepAwakePinger(@Value("${app.keep-awake-url}") String url) {
        this.target = URI.create(url.trim());
        log.info("Keep-awake ping enabled for {}", target);
    }

    /** One GET; a failure is logged and never propagates (the scheduler keeps going). Returns the status, or -1. */
    @Scheduled(fixedDelayString = "${app.keep-awake-interval:PT10M}", initialDelayString = "${app.keep-awake-interval:PT10M}")
    public int ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder(target).timeout(REQUEST_TIMEOUT).GET().build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            log.debug("Keep-awake ping {} -> {}", target, status);
            return status;
        } catch (Exception e) {
            log.warn("Keep-awake ping {} failed: {}", target, e.toString());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
    }
}
