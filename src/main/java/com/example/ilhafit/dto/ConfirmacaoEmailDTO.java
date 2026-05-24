package com.example.ilhafit.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ConfirmacaoEmailDTO {

    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    private String email;

    @NotBlank(message = "Codigo e obrigatorio")
    @Pattern(regexp = "\\d{6}", message = "Codigo deve conter 6 digitos")
    private String codigo;
}
