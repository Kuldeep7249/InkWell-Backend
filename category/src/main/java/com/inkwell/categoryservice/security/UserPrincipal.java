package com.inkwell.categoryservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String username;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal of(Long userId, String username, List<String> roles) {
        return new UserPrincipal(
                userId,
                username,
                roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()
        );
    }
}
