package com.example.ilhafit.dto.usuario;

import com.example.ilhafit.validation.SenhaForte;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioAtualizacaoDTO {

    private String nome;

    @Email(message = "Email inválido")
    private String email;

    @SenhaForte
    private String senha;
}
