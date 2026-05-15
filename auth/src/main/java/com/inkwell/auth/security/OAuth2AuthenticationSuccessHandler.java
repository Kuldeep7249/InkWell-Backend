package com.inkwell.auth.security;

import com.inkwell.auth.dto.AuthResponse;
import com.inkwell.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectProvider<AuthService> authServiceProvider;

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/success}")
    private String authorizedRedirectUri;

    @Value("${app.oauth2.failure-redirect-uri:http://localhost:5173/login?error=oauth}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        System.out.println("=== OAuth2 Success Handler reached ===");

        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            System.out.println("Provider: " + oauthToken.getAuthorizedClientRegistrationId());
            System.out.println("Attributes: " + oauthToken.getPrincipal().getAttributes());

            AuthResponse authResponse = authServiceProvider.getObject().oauth2Login(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getPrincipal().getAttributes()
            );
            System.out.println("=== oauth2Login succeeded, userId: " + authResponse.getUserId() + " ===");

            String redirectUrl = UriComponentsBuilder.fromUriString(authorizedRedirectUri)
                    .queryParam("token", authResponse.getAccessToken())
                    .queryParam("accessToken", authResponse.getAccessToken())
                    .queryParam("refreshToken", authResponse.getRefreshToken())
                    .queryParam("userId", authResponse.getUserId())
                    .queryParam("username", authResponse.getUsername())
                    .queryParam("email", authResponse.getEmail())
                    .queryParam("role", authResponse.getRole())
                    .build()
                    .toUriString();

            System.out.println("=== Redirecting to: " + redirectUrl + " ===");
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            System.out.println("=== ERROR in OAuth2 success handler ===");
            e.printStackTrace();
            String redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUri)
                    .queryParam("error", "oauth")
                    .queryParam("message", e.getMessage())
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        }
    }
}
