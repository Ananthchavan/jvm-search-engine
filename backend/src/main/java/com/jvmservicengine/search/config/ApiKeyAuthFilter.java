package com.jvmservicengine.search.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;


@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-API-Key";

    private final byte[] expectedKeyBytes;

    ApiKeyAuthFilter(String expectedKey) {
        this.expectedKeyBytes = expectedKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String providedKey = request.getHeader(HEADER_NAME);

        if (providedKey != null) {
            byte[] providedBytes = providedKey.getBytes(StandardCharsets.UTF_8);

            if (MessageDigest.isEqual(expectedKeyBytes, providedBytes)) {
                // Mark the request as authenticated for Spring Security
                var auth = new UsernamePasswordAuthenticationToken(
                        "api-admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                log.warn("Invalid API key from {}", request.getRemoteAddr());
                sendUnauthorized(response, "Invalid API key");
                return;
            }
        }
        // No key provided — Spring Security's authorization rules decide whether the
        // request is allowed (public endpoints pass through; protected ones get 403).

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}

