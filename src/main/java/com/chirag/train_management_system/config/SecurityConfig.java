package com.chirag.train_management_system.config;

import com.chirag.train_management_system.security.JwtFilter;
import com.chirag.train_management_system.security.LoginRateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.Map;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;
    private final UserDetailsService userDetailsService;

    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/favicon.ico",
                    "/index.html", "/register.html", "/dashboard.html",
                    "/search.html", "/book.html", "/payment.html",
                    "/bookings.html", "/pnr.html",
                    "/admin/dashboard.html", "/admin/trains.html",
                    "/admin/stations.html", "/admin/seats.html",
                    "/admin/users.html", "/admin/bookings.html",
                    "/css/**", "/js/**", "/error"
                ).permitAll()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/users/register"
                ).permitAll()
                .anyRequest().authenticated()
            )

            .userDetailsService(userDetailsService)

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status",    401,
                        "error",     "Unauthorized",
                        "message",   "Token is missing or has expired. Please log in.",
                        "timestamp", LocalDateTime.now().toString()
                    )));
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write(objectMapper.writeValueAsString(Map.of(
                        "status",    403,
                        "error",     "Forbidden",
                        "message",   "You do not have permission to perform this action.",
                        "timestamp", LocalDateTime.now().toString()
                    )));
                })
            )

            .addFilterBefore(loginRateLimitFilter,
                    UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, LoginRateLimitFilter.class);

        return http.build();
    }
}