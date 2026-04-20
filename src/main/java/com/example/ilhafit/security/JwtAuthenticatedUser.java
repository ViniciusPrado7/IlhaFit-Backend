package com.example.ilhafit.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class JwtAuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String tipo;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtAuthenticatedUser(Long id, String email, String tipo, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.tipo = tipo;
        this.authorities = authorities;
    }

    public Long getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
