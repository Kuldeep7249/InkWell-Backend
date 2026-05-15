package com.inkwell.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OAuth2RedirectController {

    @GetMapping("/api/auth/oauth2/authorization/{registrationId}")
    public RedirectView redirectToOAuth2Authorization(@PathVariable String registrationId) {
        return new RedirectView("/oauth2/authorization/" + registrationId);
    }

    @GetMapping("/api/auth/login/oauth2/code/{registrationId}")
    public RedirectView redirectToOAuth2Callback(@PathVariable String registrationId, HttpServletRequest request) {
        String redirectUrl = "/login/oauth2/code/" + registrationId;
        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isBlank()) {
            redirectUrl += "?" + queryString;
        }

        return new RedirectView(redirectUrl);
    }
}
