package com.example.ilhafit.config;

import com.example.ilhafit.entity.ActivitySchedule;
import com.example.ilhafit.entity.Address;
import com.example.ilhafit.entity.Administrator;
import com.example.ilhafit.entity.Category;
import com.example.ilhafit.entity.Establishment;
import com.example.ilhafit.entity.Evaluation;
import com.example.ilhafit.entity.PendingCategory;
import com.example.ilhafit.entity.Professional;
import com.example.ilhafit.entity.Report;
import com.example.ilhafit.entity.User;
import com.example.ilhafit.enums.PendingCategoryStatus;
import com.example.ilhafit.enums.RegistrationType;
import com.example.ilhafit.enums.ReportReason;
import com.example.ilhafit.enums.ReportStatus;
import com.example.ilhafit.enums.Role;
import com.example.ilhafit.repository.AdministratorRepository;
import com.example.ilhafit.repository.CategoryRepository;
import com.example.ilhafit.repository.EstablishmentRepository;
import com.example.ilhafit.repository.EvaluationRepository;
import com.example.ilhafit.repository.PendingCategoryRepository;
import com.example.ilhafit.repository.ProfessionalRepository;
import com.example.ilhafit.repository.ReportRepository;
import com.example.ilhafit.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Popula a base com dados de demonstracao (usuarios, profissionais, estabelecimentos,
 * avaliacoes e denuncias) para apresentacao. So roda quando app.seed.demo=true e e
 * idempotente: se o estabelecimento "Academia VP" ja existir, nao faz nada.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Senha123!";
    private static final String VP_EMAIL = "vinicius.souzaprado7@gmail.com";

    private static final List<String> CATEGORIAS = List.of(
            "Academia", "Musculacao", "Funcional", "Crossfit", "Boxe", "Muay Thai",
            "Jiu Jitsu", "Judo", "Karate", "Natacao", "Pilates", "Yoga", "Danca",
            "Spinning", "Circo", "Futebol", "Volei", "Basquete", "Corrida",
            "Ciclismo", "Ginastica", "Escalada");

    private static final String[] COMENT_POS = {
            "Excelente estrutura e profissionais super atenciosos!",
            "Ambiente limpo, equipamentos novos e bem cuidados.",
            "Melhor da região, recomendo demais!",
            "Aulas muito bem organizadas e instrutores capacitados.",
            "Equipe nota 10, sempre motivando a gente.",
            "Otimo custo-beneficio, vale cada centavo.",
            "Espaco amplo, climatizado e agradavel.",
            "Adorei o atendimento, com certeza voltarei sempre."
    };
    private static final String[] COMENT_NEU = {
            "Bom lugar, mas poderia ter mais horarios disponiveis.",
            "Estrutura ok e atendimento razoavel.",
            "Cumpre o que promete, sem grandes surpresas.",
            "Legal, porem fica bem cheio no horario de pico."
    };
    private static final String[] COMENT_NEG = {
            "Alguns equipamentos precisando de manutencao.",
            "O atendimento deixou um pouco a desejar.",
            "Achei o valor alto para o que e oferecido."
    };

    private final UserRepository usuarioRepository;
    private final ProfessionalRepository profissionalRepository;
    private final EstablishmentRepository estabelecimentoRepository;
    private final EvaluationRepository avaliacaoRepository;
    private final ReportRepository denunciaRepository;
    private final CategoryRepository categoriaRepository;
    private final PendingCategoryRepository categoriaPendenteRepository;
    private final AdministratorRepository administradorRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.seed.reset:false}")
    private boolean resetBase;

    private final Map<String, Category> categoriaMap = new LinkedHashMap<>();
    private final List<User> usuarios = new ArrayList<>();
    private final List<String> denunciantes = new ArrayList<>();

    private int seqTel = 0;
    private int seqCnpj = 0;
    private int seqCpf = 0;
    private int seqCref = 0;
    private int fotoLock = 1000;
    private int homemIdx = 0;
    private int mulherIdx = 0;
    private int usuarioIdx = 0;
    private Long adminId = null;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (resetBase) {
            log.warn("[DemoDataSeeder] app.seed.reset=true -> apagando TODOS os dados de dominio antes de semear.");
            limparBase();
        } else if (estabelecimentoRepository.existsByEmail(VP_EMAIL)) {
            log.info("[DemoDataSeeder] Dados de demonstracao já existem (Academia VP). Nada a fazer. "
                    + "Use app.seed.reset=true para apagar tudo e recriar.");
            return;
        }

        log.info("[DemoDataSeeder] Iniciando seed de dados de demonstracao...");

        adminId = administradorRepository.findAll().stream().findFirst().map(Administrator::getId).orElse(null);

        garantirCategorias();
        criarUsuarios();

        criarProfissionais();
        List<Establishment> scEstabs = criarEstabelecimentos();
        Establishment vp = criarEstabelecimentoVP();

        gerarAvaliacoesEDenunciasEstabelecimentos(scEstabs);
        // VP e o mais bem avaliado: 8 avaliacoes nota 5 e nenhuma denuncia.
        avaliar(vp, new int[] {5, 5, 5, 5, 5, 5, 5, 5});

        criarCategoriasPendentes(scEstabs.get(0), profissionalRepository.findAll().get(0));

        log.info("[DemoDataSeeder] Seed concluido com sucesso. Senha padrao dos cadastros: {}", DEMO_PASSWORD);
    }

    /**
     * Apaga todos os dados de dominio (estabelecimentos, profissionais, usuarios,
     * avaliacoes, denuncias, categorias e solicitacoes). NAO remove administradores.
     * Usa TRUNCATE ... CASCADE para respeitar as chaves estrangeiras e reiniciar os ids.
     */
    private void limparBase() {
        entityManager.createNativeQuery(
                "TRUNCATE TABLE "
                        + "denuncias, avaliacoes, categorias_pendentes, "
                        + "grade_atividade_dias, grade_atividade_periodos, grade_atividades, "
                        + "estabelecimento_fotos, estabelecimentos, profissionais, usuarios, categorias "
                        + "RESTART IDENTITY CASCADE"
        ).executeUpdate();
        entityManager.clear();
    }

    // ─── Categorias ────────────────────────────────────────────────────────────

    private void garantirCategorias() {
        List<String> todas = new ArrayList<>(CATEGORIAS);
        todas.add("Outros");
        for (String nome : todas) {
            Category categoria = categoriaRepository.findByNomeIgnoreCaseAndDeletedAtIsNull(nome)
                    .orElseGet(() -> {
                        Category nova = new Category();
                        nova.setNome(nome);
                        return categoriaRepository.save(nova);
                    });
            categoriaMap.put(nome, categoria);
        }
    }

    // ─── Usuarios (clientes / autores de avaliacoes) ─────────────────────────────

    private void criarUsuarios() {
        String[][] dados = {
                {"Ana Beatriz Souza", "ana.souza@demo.ilhafit.com"},
                {"Bruno Carvalho", "bruno.carvalho@demo.ilhafit.com"},
                {"Camila Ferreira", "camila.ferreira@demo.ilhafit.com"},
                {"Diego Almeida", "diego.almeida@demo.ilhafit.com"},
                {"Eduarda Lima", "eduarda.lima@demo.ilhafit.com"},
                {"Felipe Rocha", "felipe.rocha@demo.ilhafit.com"},
                {"Gabriela Nunes", "gabriela.nunes@demo.ilhafit.com"},
                {"Henrique Dias", "henrique.dias@demo.ilhafit.com"},
                {"Isabela Martins", "isabela.martins@demo.ilhafit.com"},
                {"Joao Pedro Alves", "joao.alves@demo.ilhafit.com"},
        };
        for (String[] d : dados) {
            User u = new User();
            u.setNome(d[0]);
            u.setEmail(d[1]);
            u.setSenha(passwordEncoder.encode(DEMO_PASSWORD));
            u.setEmailConfirmado(true);
            u.setRole(Role.USUARIO);
            usuarios.add(usuarioRepository.save(u));
            denunciantes.add(d[1]);
        }
    }

    // ─── Profissionais ──────────────────────────────────────────────────────────

    private void criarProfissionais() {
        // SP
        prof("Carlos Mendes", "MASCULINO", "Pinheiros - Sao Paulo/SP", false, List.of("Musculacao", "Funcional"));
        prof("Fernanda Ribeiro", "FEMININO", "Moema - Sao Paulo/SP", true, List.of("Pilates", "Yoga"));
        prof("Rafael Oliveira", "MASCULINO", "Tatuape - Sao Paulo/SP", false, List.of("Boxe", "Muay Thai"));
        prof("Juliana Castro", "FEMININO", "Santana - Sao Paulo/SP", false, List.of("Danca", "Ginastica"));

        // RJ
        prof("Marcelo Pereira", "MASCULINO", "Copacabana - Rio de Janeiro/RJ", false, List.of("Crossfit", "Funcional"));
        prof("Patricia Gomes", "FEMININO", "Tijuca - Rio de Janeiro/RJ", true, List.of("Yoga", "Pilates"));
        prof("Thiago Barbosa", "MASCULINO", "Barra da Tijuca - Rio de Janeiro/RJ", false, List.of("Jiu Jitsu", "Judo"));
        prof("Larissa Cardoso", "FEMININO", "Botafogo - Rio de Janeiro/RJ", false, List.of("Natacao", "Funcional"));

        // RS
        prof("Gustavo Schmidt", "MASCULINO", "Moinhos de Vento - Porto Alegre/RS", false, List.of("Musculacao", "Crossfit"));
        prof("Manuela Vargas", "FEMININO", "Cidade Baixa - Porto Alegre/RS", false, List.of("Danca", "Spinning"));
        prof("Leonardo Fischer", "MASCULINO", "Centro - Caxias do Sul/RS", false, List.of("Boxe", "Karate"));
        prof("Bianca Moraes", "FEMININO", "Zona Sul - Porto Alegre/RS", true, List.of("Pilates", "Ginastica"));

        // PR
        prof("Andre Kowalski", "MASCULINO", "Batel - Curitiba/PR", false, List.of("Musculacao", "Funcional"));
        prof("Carolina Bianchi", "FEMININO", "Agua Verde - Curitiba/PR", false, List.of("Natacao", "Yoga"));
        prof("Vinicius Ramos", "MASCULINO", "Centro - Londrina/PR", false, List.of("Crossfit", "Corrida"));
        prof("Amanda Reis", "FEMININO", "Centro - Maringa/PR", true, List.of("Danca", "Pilates"));

        // SC (10)
        prof("Rodrigo Espindola", "MASCULINO", "Centro - Florianópolis/SC", false, List.of("Musculacao", "Funcional"));
        prof("Mariana Cunha", "FEMININO", "Trindade - Florianópolis/SC", true, List.of("Yoga", "Pilates"));
        prof("Lucas Bittencourt", "MASCULINO", "Lagoa da Conceicao - Florianópolis/SC", false, List.of("Natacao", "Crossfit"));
        prof("Beatriz Amaral", "FEMININO", "Ingleses - Florianópolis/SC", false, List.of("Danca", "Ginastica"));
        prof("Matheus Goulart", "MASCULINO", "Canasvieiras - Florianópolis/SC", false, List.of("Boxe", "Muay Thai"));
        prof("Natalia Duarte", "FEMININO", "Kobrasol - São José/SC", true, List.of("Pilates", "Funcional"));
        prof("Guilherme Souza", "MASCULINO", "Campinas - São José/SC", false, List.of("Jiu Jitsu", "Judo"));
        prof("Priscila Fontes", "FEMININO", "Barreiros - São José/SC", false, List.of("Natacao", "Spinning"));
        prof("Otavio Machado", "MASCULINO", "Centro - Joinville/SC", false, List.of("Crossfit", "Corrida"));
        prof("Renata Steil", "FEMININO", "Centro - Balneario Camboriu/SC", false, List.of("Danca", "Yoga"));

        // Algumas avaliacoes/denuncias em profissionais de SC (para tela de moderacao).
        List<Professional> todos = profissionalRepository.findAll();
        List<Professional> scPros = todos.stream()
                .filter(p -> p.getRegiao() != null && p.getRegiao().endsWith("/SC"))
                .toList();
        int i = 0;
        for (Professional p : scPros) {
            int[] notas = (i % 2 == 0) ? new int[] {5, 4, 5} : new int[] {4, 5, 4};
            List<Evaluation> avals = avaliar(p, notas);
            if (i < 3) {
                denunciar(avals, 2, i == 0 ? 1 : 0);
            }
            i++;
        }
    }

    private Professional prof(String nome, String sexo, String regiao, boolean exclusivoMulheres, List<String> cats) {
        Professional p = new Professional();
        p.setNome(nome);
        p.setEmail(emailDe(nome));
        p.setSenha(passwordEncoder.encode(DEMO_PASSWORD));
        p.setEmailConfirmado(true);
        p.setTelefone(proximoTelefone());
        p.setCpf(proximoCpf());
        p.setSexo(sexo);
        p.setRegistroCref(proximoCref());
        p.setRegiao(regiao);
        p.setRole(RegistrationType.PROFISSIONAL);
        p.setExclusivoMulheres(exclusivoMulheres);
        p.setFotoUrl(fotoProfissional(sexo));
        p.setGradeAtividades(grade(cats, exclusivoMulheres));
        return profissionalRepository.save(p);
    }

    // ─── Estabelecimentos ────────────────────────────────────────────────────────

    private List<Establishment> criarEstabelecimentos() {
        // SP (4)
        estab("IronFit Pinheiros", "IronFit Academia LTDA", "iron.pinheiros@demo.ilhafit.com",
                addr("Rua dos Pinheiros", "1200", "Pinheiros", "Sao Paulo", "SP", "05422012", -23.5629, -46.6944),
                List.of("Academia", "Musculacao", "Funcional"), 1, "gym,fitness");
        estab("Studio Moema Fit", "Moema Fitness LTDA", "moema.fit@demo.ilhafit.com",
                addr("Avenida Ibirapuera", "2100", "Moema", "Sao Paulo", "SP", "04029200", -23.6011, -46.6553),
                List.of("Pilates", "Yoga", "Funcional"), 1, "yoga,pilates");
        estab("Tatuape Boxe Club", "Tatuape Fight LTDA", "tatuape.boxe@demo.ilhafit.com",
                addr("Rua Tuiuti", "890", "Tatuape", "Sao Paulo", "SP", "03081000", -23.5405, -46.5760),
                List.of("Boxe", "Muay Thai"), 1, "boxing,gym");
        estab("Santana Aquatic", "Santana Aquatic Center LTDA", "santana.aqua@demo.ilhafit.com",
                addr("Avenida Cruzeiro do Sul", "1500", "Santana", "Sao Paulo", "SP", "02031000", -23.5030, -46.6250),
                List.of("Natacao"), 1, "swimming,pool");

        // RJ (4)
        estab("Copa Cross", "Copa Cross Treinamento LTDA", "copa.cross@demo.ilhafit.com",
                addr("Avenida Nossa Senhora de Copacabana", "600", "Copacabana", "Rio de Janeiro", "RJ", "22020001", -22.9711, -43.1822),
                List.of("Crossfit", "Funcional"), 1, "crossfit,gym");
        estab("Tijuca Power Gym", "Tijuca Power LTDA", "tijuca.gym@demo.ilhafit.com",
                addr("Rua Conde de Bonfim", "455", "Tijuca", "Rio de Janeiro", "RJ", "20520051", -22.9245, -43.2320),
                List.of("Academia", "Musculacao"), 1, "gym,fitness");
        estab("Barra Jiu Jitsu", "Barra JJ Academia LTDA", "barra.jj@demo.ilhafit.com",
                addr("Avenida das Americas", "5000", "Barra da Tijuca", "Rio de Janeiro", "RJ", "22640102", -23.0000, -43.3650),
                List.of("Jiu Jitsu", "Judo"), 1, "martialarts,training");
        estab("Botafogo Dance Studio", "Botafogo Dance LTDA", "botafogo.dance@demo.ilhafit.com",
                addr("Rua Voluntarios da Patria", "340", "Botafogo", "Rio de Janeiro", "RJ", "22270000", -22.9519, -43.1866),
                List.of("Danca", "Ginastica"), 1, "dance,studio");

        // RS (4)
        estab("Moinhos Strength", "Moinhos Strength LTDA", "moinhos.str@demo.ilhafit.com",
                addr("Rua Padre Chagas", "220", "Moinhos de Vento", "Porto Alegre", "RS", "90570080", -30.0248, -51.2050),
                List.of("Musculacao", "Crossfit"), 1, "gym,fitness");
        estab("Cidade Baixa Yoga", "Cidade Baixa Bem Estar LTDA", "cb.yoga@demo.ilhafit.com",
                addr("Avenida Joao Pessoa", "780", "Cidade Baixa", "Porto Alegre", "RS", "90040000", -30.0430, -51.2180),
                List.of("Yoga", "Pilates"), 1, "yoga,pilates");
        estab("Caxias Fight House", "Caxias Fight LTDA", "caxias.fight@demo.ilhafit.com",
                addr("Rua Sinimbu", "1500", "Centro", "Caxias do Sul", "RS", "95020000", -29.1680, -51.1790),
                List.of("Boxe", "Karate"), 1, "boxing,gym");
        estab("Zona Sul Runners", "Zona Sul Runners LTDA", "zs.runners@demo.ilhafit.com",
                addr("Avenida Wenceslau Escobar", "2100", "Tristeza", "Porto Alegre", "RS", "91900000", -30.1080, -51.2440),
                List.of("Corrida", "Funcional"), 1, "running,track");

        // PR (4)
        estab("Batel Prime Fitness", "Batel Prime LTDA", "batel.prime@demo.ilhafit.com",
                addr("Avenida do Batel", "1750", "Batel", "Curitiba", "PR", "80420090", -25.4410, -49.2900),
                List.of("Academia", "Funcional"), 1, "gym,fitness");
        estab("Agua Verde Aqua", "Agua Verde Aqua LTDA", "av.aqua@demo.ilhafit.com",
                addr("Avenida Republica Argentina", "900", "Agua Verde", "Curitiba", "PR", "80620010", -25.4560, -49.2870),
                List.of("Natacao", "Yoga"), 1, "swimming,pool");
        estab("Londrina Crossbox", "Londrina Crossbox LTDA", "ldn.cross@demo.ilhafit.com",
                addr("Avenida Higienopolis", "1200", "Centro", "Londrina", "PR", "86020080", -23.3100, -51.1620),
                List.of("Crossfit", "Corrida"), 1, "crossfit,gym");
        estab("Maringa Dance", "Maringa Dance LTDA", "mga.dance@demo.ilhafit.com",
                addr("Avenida Brasil", "3200", "Centro", "Maringa", "PR", "87013000", -23.4250, -51.9380),
                List.of("Danca", "Pilates"), 1, "dance,studio");

        // SC (10) — 8 na Grande Florianópolis (Floripa + São José), 2 no restante do estado.
        List<Establishment> sc = new ArrayList<>();
        sc.add(estab("Floripa Center Fit", "Floripa Center Fit LTDA", "floripa.center@demo.ilhafit.com",
                addr("Rua Felipe Schmidt", "300", "Centro", "Florianópolis", "SC", "88010001", -27.5954, -48.5480),
                List.of("Academia", "Musculacao", "Funcional", "Crossfit"), 6, "gym,fitness"));
        sc.add(estab("Trindade Yoga Space", "Trindade Bem Estar LTDA", "trindade.yoga@demo.ilhafit.com",
                addr("Rua Lauro Linhares", "1100", "Trindade", "Florianópolis", "SC", "88036001", -27.5860, -48.5230),
                List.of("Yoga", "Pilates"), 1, "yoga,pilates"));
        sc.add(estab("Lagoa Surf & Fit", "Lagoa Surf Fit LTDA", "lagoa.fit@demo.ilhafit.com",
                addr("Avenida das Rendeiras", "500", "Lagoa da Conceicao", "Florianópolis", "SC", "88062000", -27.6040, -48.4670),
                List.of("Natacao", "Funcional", "Crossfit"), 2, "swimming,pool"));
        sc.add(estab("Ingleses Beach Gym", "Ingleses Beach LTDA", "ingleses.gym@demo.ilhafit.com",
                addr("Rua das Gaivotas", "820", "Ingleses", "Florianópolis", "SC", "88058000", -27.4360, -48.3960),
                List.of("Academia", "Musculacao"), 1, "gym,fitness"));
        sc.add(estab("Canasvieiras Combat", "Canasvieiras Combat LTDA", "cana.combat@demo.ilhafit.com",
                addr("Avenida das Nacoes", "410", "Canasvieiras", "Florianópolis", "SC", "88054000", -27.4280, -48.4590),
                List.of("Boxe", "Muay Thai", "Jiu Jitsu"), 1, "boxing,gym"));
        sc.add(estab("Kobrasol Prime", "Kobrasol Prime LTDA", "kobrasol.prime@demo.ilhafit.com",
                addr("Avenida Central", "760", "Kobrasol", "São José", "SC", "88102000", -27.5960, -48.6180),
                List.of("Academia", "Musculacao", "Funcional"), 2, "gym,fitness"));
        sc.add(estab("Campinas Aqua Center", "Campinas Aqua LTDA", "campinas.aqua@demo.ilhafit.com",
                addr("Avenida Josue Di Bernardi", "1200", "Campinas", "São José", "SC", "88101000", -27.5920, -48.6120),
                List.of("Natacao", "Pilates"), 1, "swimming,pool"));
        sc.add(estab("Barreiros Fight Team", "Barreiros Fight LTDA", "barreiros.fight@demo.ilhafit.com",
                addr("Rua Koesa", "150", "Barreiros", "São José", "SC", "88117000", -27.5820, -48.6350),
                List.of("Jiu Jitsu", "Judo", "Karate"), 1, "martialarts,training"));
        sc.add(estab("Joinville Iron House", "Joinville Iron LTDA", "jve.iron@demo.ilhafit.com",
                addr("Rua das Palmeiras", "700", "Centro", "Joinville", "SC", "89201000", -26.3040, -48.8460),
                List.of("Academia", "Crossfit"), 1, "crossfit,gym"));
        sc.add(estab("BC Beach Sports", "Balneario Beach Sports LTDA", "bc.beach@demo.ilhafit.com",
                addr("Avenida Brasil", "2500", "Centro", "Balneario Camboriu", "SC", "88330000", -26.9900, -48.6350),
                List.of("Funcional", "Volei", "Corrida"), 1, "beach,volleyball"));
        return sc;
    }

    private Establishment criarEstabelecimentoVP() {
        return estab("Academia VP", "Academia VP", VP_EMAIL,
                addr("Rua Manoel Loureiro", "1500", "Forquilhinhas", "São José", "SC", "88111120", -27.5595, -48.6390),
                List.of("Academia", "Boxe", "Jiu Jitsu", "Muay Thai", "Natacao", "Funcional"), 6, "gym,fitness");
    }

    private Address addr(String rua, String numero, String bairro, String cidade,
                         String uf, String cep, double lat, double lng) {
        Address a = new Address();
        a.setRua(rua);
        a.setNumero(numero);
        a.setComplemento(null);
        a.setBairro(bairro);
        a.setCidade(cidade);
        a.setEstado(uf);
        a.setCep(cep);
        a.setLatitude(lat);
        a.setLongitude(lng);
        return a;
    }

    private Establishment estab(String nome, String razao, String email, Address end,
                                List<String> cats, int nFotos, String keywordFoto) {
        Establishment e = new Establishment();
        e.setNomeFantasia(nome);
        e.setRazaoSocial(razao);
        e.setEmail(email);
        e.setSenha(passwordEncoder.encode(DEMO_PASSWORD));
        e.setEmailConfirmado(true);
        e.setTelefone(proximoTelefone());
        e.setCnpj(proximoCnpj());
        e.setRole(RegistrationType.ESTABELECIMENTO);
        e.setEndereco(end);
        e.setGradeAtividades(grade(cats, false));
        e.setFotosUrl(fotosEstabelecimento(keywordFoto, nFotos));
        return estabelecimentoRepository.save(e);
    }

    // ─── Avaliacoes e denuncias ──────────────────────────────────────────────────

    private void gerarAvaliacoesEDenunciasEstabelecimentos(List<Establishment> scEstabs) {
        // Padroes de nota que resultam em media alta (mas sempre abaixo de 5,0 para
        // manter a Academia VP como a mais bem avaliada).
        int[][] padroes5 = {
                {5, 4, 5, 4, 4},
                {4, 5, 4, 5, 3},
                {5, 5, 4, 4, 3},
                {4, 4, 5, 3, 5},
                {5, 4, 3, 4, 5},
        };
        for (int i = 0; i < scEstabs.size(); i++) {
            Establishment est = scEstabs.get(i);
            boolean especial = (i == 0 || i == 5); // Floripa Center Fit e Kobrasol Prime
            if (especial) {
                List<Evaluation> avals = avaliar(est, new int[] {5, 4, 5, 4, 3, 5});
                denunciar(avals, 11, 3); // 3 das 11 denuncias ja revisadas
            } else {
                List<Evaluation> avals = avaliar(est, padroes5[i % padroes5.length]);
                denunciar(avals, 2, i == 1 ? 1 : 0); // uma delas com denuncia ja revisada
            }
        }

        // Uma unica avaliacao com muitas denuncias em cada estabelecimento especial,
        // para demonstrar o agrupamento de denuncias por avaliacao na moderacao.
        denunciar(avaliar(scEstabs.get(0), new int[] {2}), 10, 0); // 10 denuncias numa avaliacao (Floripa Center Fit)
        denunciar(avaliar(scEstabs.get(5), new int[] {2}), 9, 0);  // 9 denuncias em outra (Kobrasol Prime)
    }

    private List<Evaluation> avaliar(Object alvo, int[] notas) {
        List<Evaluation> salvas = new ArrayList<>();
        for (int i = 0; i < notas.length; i++) {
            User autor = usuarios.get(usuarioIdx++ % usuarios.size());
            Evaluation e = new Evaluation();
            e.setNota(notas[i]);
            e.setComentario(comentarioPara(notas[i], i));
            e.setAutor(autor);
            e.setAutorId(autor.getId());
            e.setAutorEmail(autor.getEmail());
            e.setAutorNome(autor.getNome());
            e.setAutorTipo(RegistrationType.USUARIO.name());
            if (alvo instanceof Establishment est) {
                e.setEstabelecimento(est);
            } else if (alvo instanceof Professional p) {
                e.setProfissional(p);
            }
            salvas.add(avaliacaoRepository.save(e));
        }
        return salvas;
    }

    private void denunciar(List<Evaluation> avals, int total, int revisadas) {
        if (avals.isEmpty()) {
            return;
        }
        int n = avals.size();
        int[] usadasPorAvaliacao = new int[n];
        ReportReason[] motivos = ReportReason.values();
        for (int i = 0; i < total; i++) {
            int idx = i % n;
            int k = usadasPorAvaliacao[idx]++;
            String email = denunciantes.get(k % denunciantes.size());
            Report r = new Report();
            r.setAvaliacao(avals.get(idx));
            r.setDenuncianteEmail(email);
            r.setMotivo(motivos[i % motivos.length]);
            r.setDescricaoAdicional("Denuncia registrada para moderacao (dados de demonstracao).");
            if (i < revisadas) {
                r.setStatus(ReportStatus.REVISADO);
                r.setResolvedAt(LocalDateTime.now());
                r.setResolvedBy(adminId);
            } else {
                r.setStatus(ReportStatus.PENDENTE);
            }
            denunciaRepository.save(r);
        }
    }

    // ─── Categorias pendentes (fluxo de aprovacao/rejeicao no admin) ─────────────

    private void criarCategoriasPendentes(Establishment est, Professional prof) {
        // Duas solicitacoes aguardando analise (o admin pode aprovar/rejeitar na demo).
        categoriaPendente("Beach Tennis", RegistrationType.ESTABELECIMENTO, est.getId(), est.getEmail(),
                PendingCategoryStatus.PENDENTE, null);
        categoriaPendente("Kitesurf", RegistrationType.PROFISSIONAL, prof.getId(), prof.getEmail(),
                PendingCategoryStatus.PENDENTE, null);
        // Uma ja analisada (rejeitada) para mostrar o historico com observacao do admin.
        categoriaPendente("Parkour", RegistrationType.ESTABELECIMENTO, est.getId(), est.getEmail(),
                PendingCategoryStatus.REJEITADA, "Categoria muito ampla; especifique a modalidade.");
    }

    private void categoriaPendente(String nome, RegistrationType tipo, Long solicitanteId,
                                   String email, PendingCategoryStatus status, String observacao) {
        PendingCategory pc = new PendingCategory();
        pc.setNome(nome);
        pc.setTipoSolicitante(tipo);
        pc.setSolicitanteId(solicitanteId);
        pc.setStatus(status);
        pc.setEmailSnapshot(email);
        if (status != PendingCategoryStatus.PENDENTE) {
            pc.setDataAnalise(LocalDateTime.now());
            pc.setObservacaoAdmin(observacao);
        }
        categoriaPendenteRepository.save(pc);
    }

    private String comentarioPara(int nota, int i) {
        if (nota >= 4) {
            return COMENT_POS[i % COMENT_POS.length];
        }
        if (nota == 3) {
            return COMENT_NEU[i % COMENT_NEU.length];
        }
        return COMENT_NEG[i % COMENT_NEG.length];
    }

    // ─── Helpers de grade, fotos e dados fake ────────────────────────────────────

    private List<ActivitySchedule> grade(List<String> cats, boolean exclusivoMulheres) {
        List<ActivitySchedule> gs = new ArrayList<>();
        List<List<String>> diasVariacoes = List.of(
                List.of("SEGUNDA", "QUARTA", "SEXTA"),
                List.of("TERCA", "QUINTA"),
                List.of("SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA"),
                List.of("SABADO", "DOMINGO"));
        List<List<String>> periodosVariacoes = List.of(
                List.of("MANHA", "NOITE"),
                List.of("TARDE"),
                List.of("MANHA", "TARDE", "NOITE"));
        int i = 0;
        for (String c : cats) {
            Category categoria = categoriaMap.get(c);
            if (categoria == null) {
                continue;
            }
            ActivitySchedule a = new ActivitySchedule();
            a.setCategoria(categoria);
            a.setExclusivoMulheres(exclusivoMulheres);
            a.setDiasSemana(new ArrayList<>(diasVariacoes.get(i % diasVariacoes.size())));
            a.setPeriodos(new ArrayList<>(periodosVariacoes.get(i % periodosVariacoes.size())));
            gs.add(a);
            i++;
        }
        return gs;
    }

    private List<String> fotosEstabelecimento(String keyword, int n) {
        List<String> fotos = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            fotos.add("https://loremflickr.com/800/600/" + keyword + "?lock=" + (fotoLock++));
        }
        return fotos;
    }

    private String fotoProfissional(String sexo) {
        if ("FEMININO".equalsIgnoreCase(sexo)) {
            return "https://randomuser.me/api/portraits/women/" + (mulherIdx++) + ".jpg";
        }
        return "https://randomuser.me/api/portraits/men/" + (homemIdx++) + ".jpg";
    }

    private String emailDe(String nome) {
        String primeiroNome = nome.trim().split("\\s+")[0];
        String base = java.text.Normalizer.normalize(primeiroNome, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z]", "");
        return "prof." + base + "@ilhafit.com";
    }

    private String proximoTelefone() {
        return String.format("4899%07d", ++seqTel);
    }

    private String proximoCnpj() {
        return String.format("%014d", 10000000000000L + (++seqCnpj));
    }

    private String proximoCpf() {
        return String.format("%011d", 10000000000L + (++seqCpf));
    }

    private String proximoCref() {
        return String.format("%06d-G/SC", 100000 + (++seqCref));
    }
}
