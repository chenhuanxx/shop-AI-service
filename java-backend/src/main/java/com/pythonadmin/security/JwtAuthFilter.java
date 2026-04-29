package com.pythonadmin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            response.setHeader("X-Debug-Jwt", "seen");
            String token = header.substring("Bearer ".length()).trim();
            try {
                Jws<Claims> jws = jwtService.parse(token);
                String sub = jws.getPayload().getSubject();
                if (sub != null && !sub.isBlank()) {
                    response.setHeader("X-Debug-Sub", sub);
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(sub, null, List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String p = request.getRequestURI();
        if (p == null) return false;
        return "/health".equals(p) ||
            "/auth/login".equals(p) ||
            "/auth/register".equals(p) ||
            "/auth/wecom/login".equals(p) ||
            "/auth/weixin/login".equals(p);
    }
}
