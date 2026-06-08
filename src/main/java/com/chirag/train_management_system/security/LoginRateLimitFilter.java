package com.chirag.train_management_system.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";

    // Username based — 5 baar galat → 15 min block
    private static final int  USERNAME_MAX_ATTEMPTS   = 5;
    private static final long USERNAME_BLOCK_MS       = 15 * 60 * 1000L;

    // IP based — 20 baar galat → 30 min block
    private static final int  IP_MAX_ATTEMPTS         = 20;
    private static final long IP_BLOCK_MS             = 30 * 60 * 1000L;

    // Username tracking
    private final ConcurrentHashMap<String, AtomicInteger> usernameAttemptMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>          usernameBlockMap   = new ConcurrentHashMap<>();

    // IP tracking
    private final ConcurrentHashMap<String, AtomicInteger> ipAttemptMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>          ipBlockMap   = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!LOGIN_PATH.equals(request.getRequestURI()) ||
                !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Body cache karo
        CachedBodyHttpServletRequest cachedRequest =
                new CachedBodyHttpServletRequest(request);

        String clientIp = getClientIp(request);
        String username = extractUsername(cachedRequest);

        // ── IP Block Check ────────────────────────────────────────────────
        Long ipBlockExpiry = ipBlockMap.get(clientIp);
        if (ipBlockExpiry != null) {
            if (System.currentTimeMillis() < ipBlockExpiry) {
                long minutesLeft = (ipBlockExpiry - System.currentTimeMillis()) / 60000 + 1;
                log.warn("IP blocked: {}", clientIp);
                sendBlockedResponse(response,
                        "Too many failed attempts from this network. " +
                        "Please try again after " + minutesLeft + " minute(s).");
                return;
            } else {
                ipBlockMap.remove(clientIp);
                ipAttemptMap.remove(clientIp);
            }
        }

        // ── Username Block Check ──────────────────────────────────────────
        if (username != null && !username.isBlank()) {
            String usernameKey = username.toLowerCase().trim();
            Long usernameBlockExpiry = usernameBlockMap.get(usernameKey);
            if (usernameBlockExpiry != null) {
                if (System.currentTimeMillis() < usernameBlockExpiry) {
                    long minutesLeft =
                            (usernameBlockExpiry - System.currentTimeMillis()) / 60000 + 1;
                    log.warn("Username blocked: {}", usernameKey);
                    sendBlockedResponse(response,
                            "Too many failed login attempts. " +
                            "Please try again after " + minutesLeft + " minute(s).");
                    return;
                } else {
                    usernameBlockMap.remove(usernameKey);
                    usernameAttemptMap.remove(usernameKey);
                }
            }
        }

        // ── Request aage bhejo ────────────────────────────────────────────
        StatusCapturingResponseWrapper wrappedResponse =
                new StatusCapturingResponseWrapper(response);

        filterChain.doFilter(cachedRequest, wrappedResponse);

        // ── Response check karo ───────────────────────────────────────────
        if (wrappedResponse.getStatus() == HttpStatus.UNAUTHORIZED.value()) {

            // IP attempt count
            AtomicInteger ipAttempts = ipAttemptMap.computeIfAbsent(
                    clientIp, k -> new AtomicInteger(0));
            int ipCount = ipAttempts.incrementAndGet();
            log.warn("Failed login from IP {}: {}/{}", clientIp, ipCount, IP_MAX_ATTEMPTS);

            if (ipCount >= IP_MAX_ATTEMPTS) {
                ipBlockMap.put(clientIp,
                        System.currentTimeMillis() + IP_BLOCK_MS);
                ipAttemptMap.remove(clientIp);
                log.warn("IP blocked 30 min: {}", clientIp);
            }

            // Username attempt count
            if (username != null && !username.isBlank()) {
                String usernameKey = username.toLowerCase().trim();
                AtomicInteger usernameAttempts = usernameAttemptMap.computeIfAbsent(
                        usernameKey, k -> new AtomicInteger(0));
                int usernameCount = usernameAttempts.incrementAndGet();
                log.warn("Failed login for username {}: {}/{}", 
                    usernameKey, usernameCount, USERNAME_MAX_ATTEMPTS);

                if (usernameCount >= USERNAME_MAX_ATTEMPTS) {
                    usernameBlockMap.put(usernameKey,
                            System.currentTimeMillis() + USERNAME_BLOCK_MS);
                    usernameAttemptMap.remove(usernameKey);
                    log.warn("Username blocked 15 min: {}", usernameKey);
                }
            }

        } else if (wrappedResponse.getStatus() == HttpStatus.OK.value()) {
            // Successful login — dono reset karo
            ipAttemptMap.remove(clientIp);
            ipBlockMap.remove(clientIp);
            if (username != null && !username.isBlank()) {
                String usernameKey = username.toLowerCase().trim();
                usernameAttemptMap.remove(usernameKey);
                usernameBlockMap.remove(usernameKey);
            }
            log.info("Successful login for: {}", username);
        }
    }

    private String extractUsername(CachedBodyHttpServletRequest request) {
        try {
            byte[] body = request.getInputStream().readAllBytes();
            JsonNode node = objectMapper.readTree(
                    new String(body, StandardCharsets.UTF_8));
            return node.path("username").asText(null);
        } catch (Exception e) {
            log.warn("Could not extract username: {}", e.getMessage());
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendBlockedResponse(HttpServletResponse response,
                                     String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status",    429,
                "error",     "Too Many Requests",
                "message",   message,
                "timestamp", LocalDateTime.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}