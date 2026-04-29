package com.pythonadmin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + 
                        (ex != null ? ex.getMessage().replace("\"", "'") : "Authentication required") + "\"}");
                })
            )
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/health", "/auth/**", "/error", "/error/**", "/uploads/**",
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/docs").permitAll()
                // 公开访问：产品列表、分类和详情
                .requestMatchers(HttpMethod.GET, "/products", "/products/{id}", "/products/categories").permitAll()
                // 公开访问：Banner列表
                .requestMatchers(HttpMethod.GET, "/banners", "/banners/{id}", "/banners/enabled").permitAll()
                .requestMatchers("/upload").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoder spring = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        PasswordEncoder passlib = new PasslibPbkdf2Sha256PasswordEncoder(29000, 16);

        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return passlib.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (encodedPassword != null && encodedPassword.startsWith("$pbkdf2-sha256$")) {
                    return passlib.matches(rawPassword, encodedPassword);
                }
                return spring.matches(rawPassword, encodedPassword);
            }
        };
    }
}
