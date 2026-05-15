package com.marcog.peluqueria.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro defensivo de coste bajo: corta scrapers conocidos y limita ráfagas por IP/ruta.
 * Protege especialmente endpoints caros como autenticación y chatbot antes de llegar a la lógica de negocio.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AntiAbuseFilter extends OncePerRequestFilter {

    private static final Set<String> BOT_USER_AGENT_TOKENS = Set.of(
            "ahrefs", "bingbot", "bot", "crawler", "facebookexternalhit",
            "masscan", "nikto", "scrapy", "semrush", "spider", "zgrab"
    );

    private final Map<String, FixedWindow> windows = new ConcurrentHashMap<>();

    @Value("${app.security.anti-abuse.enabled:true}")
    private boolean enabled;

    @Value("${app.security.anti-abuse.general-per-minute:240}")
    private int generalLimit;

    @Value("${app.security.anti-abuse.auth-per-minute:20}")
    private int authLimit;

    @Value("${app.security.anti-abuse.chat-per-minute:8}")
    private int chatLimit;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled || HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isKnownScraper(request)) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "Petición bloqueada por protección anti-abuso.");
            return;
        }

        String ip = clientIp(request);
        int limit = limitFor(path);
        String key = bucketKey(ip, path);
        if (!consume(key, limit)) {
            response.setHeader("Retry-After", "60");
            writeError(response, 429, "Demasiadas peticiones. Espera un minuto antes de intentarlo de nuevo.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isKnownScraper(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) return false;
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        return BOT_USER_AGENT_TOKENS.stream().anyMatch(normalized::contains);
    }

    private int limitFor(String path) {
        if (path.startsWith("/api/chat")) return chatLimit;
        if (path.startsWith("/api/auth")) return authLimit;
        return generalLimit;
    }

    private String bucketKey(String ip, String path) {
        if (path.startsWith("/api/chat")) return ip + ":chat";
        if (path.startsWith("/api/auth")) return ip + ":auth";
        return ip + ":api";
    }

    private boolean consume(String key, int limit) {
        long now = System.currentTimeMillis();
        FixedWindow window = windows.compute(key, (ignored, current) -> {
            if (current == null || now >= current.resetAtMillis) {
                return new FixedWindow(now + Duration.ofMinutes(1).toMillis(), 1);
            }
            current.count++;
            return current;
        });

        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(entry -> entry.getValue().resetAtMillis < now);
        }

        return window.count <= limit;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"status":%d,"code":"ANTI_ABUSE","message":"%s"}
                """.formatted(status, message));
    }

    private static class FixedWindow {
        private final long resetAtMillis;
        private int count;

        private FixedWindow(long resetAtMillis, int count) {
            this.resetAtMillis = resetAtMillis;
            this.count = count;
        }
    }
}
