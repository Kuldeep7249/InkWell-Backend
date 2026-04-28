package com.inkwell.notification.config;

import com.inkwell.notification.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/notifications/send",
                                "/notification-api/send"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/notifications/bulk",
                                "/api/notifications/email",
                                "/notification-api/bulk",
                                "/notification-api/email"
                        ).hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/notifications/recipient/*",
                                "/api/notifications/recipient/*/unread-count",
                                "/api/notifications/me",
                                "/api/notifications/me/unread-count",
                                "/notification-api/recipient/*",
                                "/notification-api/recipient/*/unread-count",
                                "/notification-api/me",
                                "/notification-api/me/unread-count"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/notifications/*/read",
                                "/api/notifications/recipient/*/read-all",
                                "/api/notifications/me/read-all",
                                "/notification-api/*/read",
                                "/notification-api/recipient/*/read-all",
                                "/notification-api/me/read-all"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/notifications/*",
                                "/api/notifications/recipient/*/read",
                                "/api/notifications/me/read",
                                "/notification-api/*",
                                "/notification-api/recipient/*/read",
                                "/notification-api/me/read"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/notifications/all",
                                "/notification-api/all"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\",\"data\":null}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
