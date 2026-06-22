package com.example.ilhafit.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Cobertura dos métodos de JwtAuthenticatedUser — especialmente os boolean UserDetails */
class JwtAuthenticatedUserTest {

    private JwtAuthenticatedUser user;

    @BeforeEach
    void setUp() {
        user = new JwtAuthenticatedUser(
                42L,
                "usuario@ilhafit.com",
                "USUARIO",
                List.of(new SimpleGrantedAuthority("USUARIO"), new SimpleGrantedAuthority("ROLE_USUARIO")));
    }

    @Test
    void getId_retornaIdCorreto() {
        assertThat(user.getId()).isEqualTo(42L);
    }

    @Test
    void getTipo_retornaTipoCorreto() {
        assertThat(user.getTipo()).isEqualTo("USUARIO");
    }

    @Test
    void getUsername_retornaEmail() {
        assertThat(user.getUsername()).isEqualTo("usuario@ilhafit.com");
    }

    @Test
    void getPassword_retornaNull() {
        assertThat(user.getPassword()).isNull();
    }

    @Test
    void getAuthorities_retornaColecaoInformada() {
        assertThat(user.getAuthorities())
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("USUARIO", "ROLE_USUARIO");
    }

    @Test
    void isAccountNonExpired_retornaTrue() {
        assertThat(user.isAccountNonExpired()).isTrue();
    }

    @Test
    void isAccountNonLocked_retornaTrue() {
        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void isCredentialsNonExpired_retornaTrue() {
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void isEnabled_retornaTrue() {
        assertThat(user.isEnabled()).isTrue();
    }
}
