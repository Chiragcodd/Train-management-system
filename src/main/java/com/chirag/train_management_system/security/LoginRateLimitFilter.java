package com.chirag.train_management_system.security;

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
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, AtomicInteger> attemptMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockMap = new ConcurrentHashMap<>();
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

        String clientIp = getClientIp(request);

        Long blockExpiry = blockMap.get(clientIp);
        if (blockExpiry != null) {
            if (System.currentTimeMillis() < blockExpiry) {
                long minutesLeft = (blockExpiry - System.currentTimeMillis()) / 60000;
                log.warn("Blocked login from IP: {} — {} min remaining", clientIp, minutesLeft);
                sendTooManyRequestsResponse(response,
                        "Too many failed attempts. Try again in " + minutesLeft + " minute(s).");
                return;
            } else {
                blockMap.remove(clientIp);
                attemptMap.remove(clientIp);
            }
        }

        StatusCapturingResponseWrapper wrappedResponse =
                new StatusCapturingResponseWrapper(response);

        filterChain.doFilter(request, wrappedResponse);

        if (wrappedResponse.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
            AtomicInteger attempts = attemptMap.computeIfAbsent(
                    clientIp, k -> new AtomicInteger(0));
            int count = attempts.incrementAndGet();
            log.warn("Failed login {}/{} from IP: {}", count, MAX_ATTEMPTS, clientIp);

            if (count >= MAX_ATTEMPTS) {
                blockMap.put(clientIp, System.currentTimeMillis() + BLOCK_DURATION_MS);
                attemptMap.remove(clientIp);
                log.warn("IP blocked 15 min: {}", clientIp);
            }
        } else if (wrappedResponse.getStatus() == HttpStatus.OK.value()) {
            attemptMap.remove(clientIp);
            blockMap.remove(clientIp);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response,
                                              String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "status", 429,
                "error", "Too Many Requests",
                "message", message,
                "timestamp", LocalDateTime.now().toString()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}