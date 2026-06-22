package com.example.ilhafit.integration;

import com.example.ilhafit.dto.ActivityScheduleDTO;
import com.example.ilhafit.dto.AddressDTO;
import com.example.ilhafit.dto.CategoryDTO;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.dto.user.UserRegistrationDTO;

import java.util.ArrayList;
import java.util.List;

class TestFixtures {

    static final String SENHA_PADRAO = "Senha@123";
    static final String ADMIN_EMAIL = "admin.test@ilhafit.com";
    static final String ADMIN_SENHA = "Adm@Test123";
    static final Long ID_INEXISTENTE = 999_999L;

    static UserRegistrationDTO usuarioDto(String email) {
        UserRegistrationDTO dto = new UserRegistrationDTO();
        dto.setNome("Usuario Teste");
        dto.setEmail(email);
        dto.setSenha(SENHA_PADRAO);
        dto.setConfirmacaoSenha(SENHA_PADRAO);
        return dto;
    }

    static CategoryDTO.Registro categoriaDto(String nome) {
        CategoryDTO.Registro dto = new CategoryDTO.Registro();
        dto.setNome(nome);
        return dto;
    }

    static ProfessionalDTO.Registro profissionalDto(String email, String cpf) {
        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setNome("Profissional Teste");
        dto.setEmail(email);
        dto.setSenha(SENHA_PADRAO);
        dto.setTelefone("48999887766");
        dto.setCpf(cpf);
        dto.setSexo("MASCULINO");
        dto.setRegiao("Centro");
        dto.setExclusivoMulheres(false);
        dto.setGradeAtividades(new ArrayList<>());
        dto.setFotoUrl("https://example.com/foto.jpg");
        return dto;
    }

    static ProfessionalDTO.Registro profissionalDtoComGrade(String email, String cpf,
                                                             List<ActivityScheduleDTO.Registro> grade) {
        ProfessionalDTO.Registro dto = profissionalDto(email, cpf);
        dto.setGradeAtividades(grade);
        return dto;
    }

    static EstablishmentDTO.Registro estabelecimentoDto(String email, String cnpj) {
        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setNomeFantasia("Academia Teste");
        dto.setRazaoSocial("Academia Teste LTDA");
        dto.setEmail(email);
        dto.setSenha(SENHA_PADRAO);
        dto.setTelefone("48999887700");
        dto.setCnpj(cnpj);
        dto.setEndereco(enderecoDto());
        dto.setGradeAtividades(new ArrayList<>());
        dto.setFotosUrl(new ArrayList<>(List.of("https://example.com/foto.jpg")));
        return dto;
    }

    static EstablishmentDTO.Atualizacao estabelecimentoAtualizacaoDto(String email, String cnpj) {
        EstablishmentDTO.Atualizacao dto = new EstablishmentDTO.Atualizacao();
        dto.setNomeFantasia("Academia Atualizada");
        dto.setRazaoSocial("Academia Atualizada LTDA");
        dto.setEmail(email);
        dto.setSenha(SENHA_PADRAO);
        dto.setTelefone("48999887700");
        dto.setCnpj(cnpj);
        dto.setEndereco(enderecoDto());
        dto.setGradeAtividades(new ArrayList<>());
        dto.setFotosUrl(new ArrayList<>(List.of("https://example.com/foto2.jpg")));
        return dto;
    }

    static AddressDTO enderecoDto() {
        AddressDTO dto = new AddressDTO();
        dto.setRua("Rua das Flores");
        dto.setNumero("100");
        dto.setBairro("Centro");
        dto.setCidade("Florianopolis");
        dto.setEstado("SC");
        dto.setCep("88000000");
        dto.setLatitude(-27.5949);
        dto.setLongitude(-48.5482);
        return dto;
    }

    static ActivityScheduleDTO.Registro gradeDto(Long categoriaId) {
        ActivityScheduleDTO.Registro dto = new ActivityScheduleDTO.Registro();
        dto.setCategoriaId(categoriaId);
        dto.setExclusivoMulheres(false);
        dto.setDiasSemana(new ArrayList<>(List.of("SEGUNDA", "QUARTA")));
        dto.setPeriodos(new ArrayList<>(List.of("MANHA")));
        return dto;
    }
}
