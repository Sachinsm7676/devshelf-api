package com.devshelf.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs one line per request: method, path, status and duration. Never logs bodies or query strings.
 * Health checks (polled by the host every few seconds) are logged at DEBUG only.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long millis = (System.nanoTime() - start) / 1_000_000;
            String path = request.getRequestURI();
            if (path.startsWith("/actuator/health")) {
                log.debug("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
            } else {
                log.info("{} {} -> {} ({} ms)", request.getMethod(), path, response.getStatus(), millis);
            }
        }
    }
}
