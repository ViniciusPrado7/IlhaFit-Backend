package com.example.ilhafit.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNormalizationTest {

    @Test
    void profissional_onPersist_normalizesTextFields() {
        Profissional p = new Profissional();
        p.setNome("  João Silva  ");
        p.setEmail("JOAO@EMAIL.COM");
        p.setRegiao("  Norte da Ilha  ");
        p.setSenha("Teste@123");
        p.setCpf("12345678900");
        p.setTelefone("48999887766");
        p.onCreate();

        assertThat(p.getNome()).isEqualTo("joão silva");
        assertThat(p.getEmail()).isEqualTo("joao@email.com");
        assertThat(p.getRegiao()).isEqualTo("norte da ilha");
        assertThat(p.getSenha()).isEqualTo("Teste@123");
        assertThat(p.getCpf()).isEqualTo("12345678900");
    }

    @Test
    void estabelecimento_onPersist_normalizesTextFields() {
        Estabelecimento e = new Estabelecimento();
        e.setNomeFantasia("  Academia Ilha Fit  ");
        e.setRazaoSocial("  Academia Ltda  ");
        e.setEmail("ACADEMIA@ILHA.COM");
        e.setSenha("Senha@123");
        e.setCnpj("12345678000195");
        e.setTelefone("48999887766");
        e.onCreate();

        assertThat(e.getNomeFantasia()).isEqualTo("academia ilha fit");
        assertThat(e.getRazaoSocial()).isEqualTo("academia ltda");
        assertThat(e.getEmail()).isEqualTo("academia@ilha.com");
        assertThat(e.getSenha()).isEqualTo("Senha@123");
        assertThat(e.getCnpj()).isEqualTo("12345678000195");
    }

    // EN1: normalizeFields() cobre agora os campos do Endereco embutido
    @Test
    void estabelecimento_onPersist_normalizesEndereco() {
        Estabelecimento e = new Estabelecimento();
        e.setNomeFantasia("academia");
        e.setRazaoSocial("academia ltda");
        e.setEmail("test@test.com");
        e.setSenha("Senha@123");
        e.setCnpj("12345678000195");
        e.setTelefone("48999887766");

        Endereco endereco = new Endereco();
        endereco.setRua("Rua das Flores");
        endereco.setBairro("Centro");
        endereco.setCidade("Florianópolis");
        endereco.setNumero("123");
        endereco.setEstado("SC");
        endereco.setCep("88000000");
        e.setEndereco(endereco);

        e.onCreate();

        assertThat(e.getEndereco().getRua()).isEqualTo("rua das flores");
        assertThat(e.getEndereco().getBairro()).isEqualTo("centro");
        assertThat(e.getEndereco().getCidade()).isEqualTo("florianópolis");
    }

    @Test
    void categoria_onPersistAndUpdate_normalizesNome() {
        Categoria c = new Categoria();
        c.setNome("  YOGA  ");
        c.normalizeFields();

        assertThat(c.getNome()).isEqualTo("yoga");
    }

    @Test
    void categoria_collapseSpaces() {
        Categoria c = new Categoria();
        c.setNome("Futebol  de  Praia");
        c.normalizeFields();

        assertThat(c.getNome()).isEqualTo("futebol de praia");
    }

    @Test
    void usuario_onPersist_normalizesNomeAndEmail() {
        Usuario u = new Usuario();
        u.setNome("  Maria Souza  ");
        u.setEmail("MARIA@EMAIL.COM");
        u.setSenha("Senha@123");
        u.onCreate();

        assertThat(u.getNome()).isEqualTo("maria souza");
        assertThat(u.getEmail()).isEqualTo("maria@email.com");
        assertThat(u.getSenha()).isEqualTo("Senha@123");
    }

    @Test
    void categoriaPendente_onPersist_normalizesNome() {
        CategoriaPendente cp = new CategoriaPendente();
        cp.setNome("  CrossFit  ");
        cp.setObservacaoAdmin("  Categoria Aprovada  ");
        cp.onCreate();

        assertThat(cp.getNome()).isEqualTo("crossfit");
        assertThat(cp.getObservacaoAdmin()).isEqualTo("categoria aprovada");
    }
}
