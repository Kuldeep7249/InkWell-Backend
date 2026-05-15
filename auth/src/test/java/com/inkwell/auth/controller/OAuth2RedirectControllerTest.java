package com.inkwell.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuth2RedirectControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OAuth2RedirectController()).build();
    }

    @Test
    void redirectsPrefixedAuthorizationEndpointToSpringSecurityEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/oauth2/authorization/google"));
    }

    @Test
    void redirectsPrefixedCallbackEndpointAndPreservesQueryString() throws Exception {
        mockMvc.perform(get("/api/auth/login/oauth2/code/google")
                        .queryParam("code", "abc")
                        .queryParam("state", "xyz"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login/oauth2/code/google?code=abc&state=xyz"));
    }
}
