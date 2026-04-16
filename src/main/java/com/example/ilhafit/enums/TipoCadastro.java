package com.example.ilhafit.enums;

public enum TipoCadastro {
    USUARIO("usuário"),
    ADMINISTRADOR("administrador"),
    PROFISSIONAL("profissional"),
    ESTABELECIMENTO("estabelecimento");

    private final String descricao;

    TipoCadastro(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
