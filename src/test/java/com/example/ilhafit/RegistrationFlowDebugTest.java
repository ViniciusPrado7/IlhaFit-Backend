package com.example.ilhafit;

import com.example.ilhafit.dto.AddressDTO;
import com.example.ilhafit.dto.EstablishmentDTO;
import com.example.ilhafit.dto.ProfessionalDTO;
import com.example.ilhafit.service.EstablishmentService;
import com.example.ilhafit.service.ProfessionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class RegistrationFlowDebugTest {

    @Autowired
    private EstablishmentService establishmentService;

    @Autowired
    private ProfessionalService professionalService;

    @Test
    void shouldRegisterEstablishmentWithSingleActivity() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        AddressDTO address = new AddressDTO();
        address.setRua("Rua Teste");
        address.setNumero("100");
        address.setBairro("Centro");
        address.setCidade("Florianopolis");
        address.setEstado("SC");
        address.setCep("88000000");
        address.setLatitude(-27.5949);
        address.setLongitude(-48.5482);

        EstablishmentDTO.Registro dto = new EstablishmentDTO.Registro();
        dto.setNomeFantasia("Estab " + suffix);
        dto.setRazaoSocial("Estab LTDA " + suffix);
        dto.setEmail("estab." + suffix + "@example.com");
        dto.setSenha("Senha@123");
        dto.setTelefone("48999999999");
        dto.setCnpj("99" + suffix.substring(0, 6) + "000199");
        dto.setEndereco(address);
        dto.setGradeAtividades(new ArrayList<>());
        dto.setFotosUrl(new ArrayList<>(List.of("https://example.com/image.jpg")));

        establishmentService.cadastrar(dto);
    }

    @Test
    void shouldRegisterProfessionalWithSingleActivity() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        ProfessionalDTO.Registro dto = new ProfessionalDTO.Registro();
        dto.setNome("Profissional " + suffix);
        dto.setEmail("prof." + suffix + "@example.com");
        dto.setSenha("Senha@123");
        dto.setTelefone("48999999998");
        dto.setCpf("12345" + suffix.substring(0, 6));
        dto.setSexo("MASCULINO");
        dto.setRegistroCref(null);
        dto.setRegiao("Centro");
        dto.setExclusivoMulheres(false);
        dto.setGradeAtividades(new ArrayList<>());
        dto.setFotoUrl("https://example.com/foto.jpg");

        professionalService.cadastrar(dto);
    }
}
