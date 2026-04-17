package com.example.ilhafit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnderecoDTO {

    @NotBlank(message = "Rua e obrigatoria")
    private String rua;

    @NotBlank(message = "Numero e obrigatorio")
    private String numero;

    private String complemento;

    @NotBlank(message = "Bairro e obrigatorio")
    private String bairro;

    @NotBlank(message = "Cidade e obrigatoria")
    private String cidade;

    @NotBlank(message = "Estado e obrigatorio")
    private String estado;

    @NotBlank(message = "CEP e obrigatorio")
    private String cep;

    private Double latitude;

    private Double longitude;
}
