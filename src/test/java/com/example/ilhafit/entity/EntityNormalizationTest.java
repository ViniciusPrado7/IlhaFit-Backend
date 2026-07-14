package com.example.ilhafit.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNormalizationTest {

    @Test
    void profissional_onPersist_normalizesTextFields() {
        Professional p = new Professional();
        p.setNome("  João Silva  ");
        p.setEmail("JOAO@EMAIL.COM");
        p.setRegiao("  Norte da Ilha  ");
        p.setSenha("Teste@123");
        p.setCpf("12345678900");
        p.setTelefone("48999887766");
        p.onCreate();

        assertThat(p.getNome()).isEqualTo("João Silva");
        assertThat(p.getEmail()).isEqualTo("joao@email.com");
        assertThat(p.getRegiao()).isEqualTo("Norte da Ilha");
        assertThat(p.getSenha()).isEqualTo("Teste@123");
        assertThat(p.getCpf()).isEqualTo("12345678900");
    }

    @Test
    void estabelecimento_onPersist_normalizesTextFields() {
        Establishment e = new Establishment();
        e.setNomeFantasia("  Academia Ilha Fit  ");
        e.setRazaoSocial("  Academia Ltda  ");
        e.setEmail("ACADEMIA@ILHA.COM");
        e.setSenha("Senha@123");
        e.setCnpj("12345678000195");
        e.setTelefone("48999887766");
        e.onCreate();

        assertThat(e.getNomeFantasia()).isEqualTo("Academia Ilha Fit");
        assertThat(e.getRazaoSocial()).isEqualTo("Academia Ltda");
        assertThat(e.getEmail()).isEqualTo("academia@ilha.com");
        assertThat(e.getSenha()).isEqualTo("Senha@123");
        assertThat(e.getCnpj()).isEqualTo("12345678000195");
    }

    @Test
    void categoria_onPersistAndUpdate_normalizesNome() {
        Category c = new Category();
        c.setNome("  YOGA  ");
        c.normalizeFields();

        assertThat(c.getNome()).isEqualTo("Yoga");
    }

    @Test
    void categoria_collapseSpaces() {
        Category c = new Category();
        c.setNome("Futebol  de  Praia");
        c.normalizeFields();

        assertThat(c.getNome()).isEqualTo("Futebol de Praia");
    }

    @Test
    void usuario_onPersist_normalizesNomeAndEmail() {
        User u = new User();
        u.setNome("  Maria Souza  ");
        u.setEmail("MARIA@EMAIL.COM");
        u.setSenha("Senha@123");
        u.onCreate();

        assertThat(u.getNome()).isEqualTo("Maria Souza");
        assertThat(u.getEmail()).isEqualTo("maria@email.com");
        assertThat(u.getSenha()).isEqualTo("Senha@123");
    }

    @Test
    void categoriaPendente_onPersist_normalizesNome() {
        PendingCategory cp = new PendingCategory();
        cp.setNome("  CrossFit  ");
        cp.setObservacaoAdmin("  Categoria Aprovada  ");
        cp.onCreate();

        assertThat(cp.getNome()).isEqualTo("Crossfit");
        assertThat(cp.getObservacaoAdmin()).isEqualTo("Categoria Aprovada");
    }
}
