package com.example.ilhafit.enums;

public enum RegistrationType {
    USUARIO("usuÃ¡rio"),
    ADMINISTRADOR("administrador"),
    PROFISSIONAL("profissional"),
    ESTABELECIMENTO("estabelecimento");

    private final String descricao;

    RegistrationType(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}

