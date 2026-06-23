package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressDTO {

    @NotBlank(message = "Rua e obrigatoria")
    private String rua;

    @NotBlank(message = "Numero e obrigatorio")
    @Pattern(regexp = "\\d+", message = "Numero deve conter apenas numeros")
    private String numero;

    private String complemento;

    @NotBlank(message = "Bairro e obrigatorio")
    private String bairro;

    @NotBlank(message = "Cidade e obrigatoria")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Cidade deve conter apenas letras")
    private String cidade;

    @NotBlank(message = "Estado e obrigatorio")
    @Pattern(regexp = "^(AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO)$", message = "Estado deve ser uma UF valida")
    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    private String estado;

    @NotBlank(message = "CEP e obrigatorio")
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 numeros")
    private String cep;

    private Double latitude;

    private Double longitude;
}

