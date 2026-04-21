package com.example.ilhafit.service;

import com.example.ilhafit.exception.ConteudoInadequadoException;
import com.example.ilhafit.exception.ModeracaoIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModeracaoService {

    private static final String GROQ_CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MENSAGEM_BLOQUEIO = "Sua mensagem contem conteudo ofensivo ou inadequado.";
    private static final String MENSAGEM_INDISPONIVEL = "Nao foi possivel validar o conteudo no momento.";

    private final Environment environment;
    private final ObjectMapper objectMapper;

    @Value("${groq.moderation.model:llama-3.3-70b-versatile}")
    private String groqModerationModel;

    public void validarTextoPermitido(String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }

        String apiKey = buscarGroqApiKey()
                .orElseThrow(() -> new ModeracaoIndisponivelException(
                        "Configure GROQ_API_KEY para validar mensagens ofensivas."));

        ResultadoModeracao resultado = consultarGroq(apiKey, texto);
        if (resultado.bloquear()) {
            throw new ConteudoInadequadoException(MENSAGEM_BLOQUEIO);
        }
    }

    private ResultadoModeracao consultarGroq(String apiKey, String texto) {
        try {
            GroqChatResponse response = RestClient.builder()
                    .build()
                    .post()
                    .uri(GROQ_CHAT_COMPLETIONS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GroqChatRequest.fromPrompt(groqModerationModel, promptModeracao(texto)))
                    .retrieve()
                    .body(GroqChatResponse.class);

            return parseResultado(extrairTextoGroq(response));
        } catch (RestClientResponseException e) {
            throw new ModeracaoIndisponivelException(mensagemErroGroq(e), e);
        } catch (RestClientException e) {
            throw new ModeracaoIndisponivelException(MENSAGEM_INDISPONIVEL, e);
        }
    }

    private String promptModeracao(String texto) {
        return """
                Voce e um classificador de moderacao para uma plataforma fitness.
                Analise se o texto contem xingamento, ofensa direta, ameaca, discurso de odio,
                assedio, humilhacao, linguagem sexual explicita ou conteudo inadequado para avaliacao/denuncia.
                Responda somente JSON valido, sem markdown, neste formato:
                {"bloquear":true,"motivo":"descricao curta"}
                ou
                {"bloquear":false,"motivo":"permitido"}

                Texto:
                %s
                """.formatted(texto);
    }

    private String extrairTextoGroq(GroqChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ModeracaoIndisponivelException(MENSAGEM_INDISPONIVEL);
        }

        GroqChatChoice choice = response.choices().get(0);
        if (choice.message() == null || choice.message().content() == null) {
            throw new ModeracaoIndisponivelException(MENSAGEM_INDISPONIVEL);
        }

        return choice.message().content();
    }

    private ResultadoModeracao parseResultado(String respostaTexto) {
        try {
            String json = limparJsonResposta(respostaTexto);
            JsonNode node = objectMapper.readTree(json);
            return new ResultadoModeracao(
                    node.path("bloquear").asBoolean(false),
                    node.path("motivo").asText("permitido"));
        } catch (JsonProcessingException e) {
            throw new ModeracaoIndisponivelException("A IA retornou uma resposta invalida.", e);
        }
    }

    private String limparJsonResposta(String respostaTexto) {
        String limpo = respostaTexto.trim();
        if (limpo.startsWith("```")) {
            limpo = limpo.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        return limpo.trim();
    }

    private String mensagemErroGroq(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        if (status == 400) {
            return "Requisicao invalida para a Groq API.";
        }
        if (status == 401 || status == 403) {
            return "Chave da Groq invalida, revogada ou sem permissao.";
        }
        if (status == 429) {
            return "Limite gratuito da Groq API atingido. Tente novamente mais tarde.";
        }
        if (status == 404) {
            return "Modelo da Groq nao encontrado ou indisponivel para esta chave.";
        }
        return MENSAGEM_INDISPONIVEL;
    }

    private Optional<String> buscarGroqApiKey() {
        return primeiroValorPreenchido(
                environment.getProperty("GROQ_API_KEY"),
                System.getenv("GROQ_API_KEY"),
                buscarNoEnvFile("GROQ_API_KEY")
        );
    }

    private Optional<String> primeiroValorPreenchido(String... valores) {
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return Optional.of(valor.trim());
            }
        }
        return Optional.empty();
    }

    private String buscarNoEnvFile(String nome) {
        Path envPath = Path.of(".env");
        if (!Files.exists(envPath)) {
            return null;
        }

        try {
            for (String linha : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String limpa = linha.trim();
                if (limpa.isBlank() || limpa.startsWith("#") || !limpa.startsWith(nome + "=")) {
                    continue;
                }

                return limparValorEnv(limpa.substring(nome.length() + 1));
            }
        } catch (IOException e) {
            throw new ModeracaoIndisponivelException("Nao foi possivel ler o arquivo .env.", e);
        }

        return null;
    }

    private String limparValorEnv(String valor) {
        String limpo = valor.trim();
        if ((limpo.startsWith("\"") && limpo.endsWith("\""))
                || (limpo.startsWith("'") && limpo.endsWith("'"))) {
            return limpo.substring(1, limpo.length() - 1);
        }
        return limpo;
    }

    private record ResultadoModeracao(boolean bloquear, String motivo) {
    }

    private record GroqChatRequest(
            String model,
            List<GroqChatMessage> messages,
            Double temperature,
            @JsonProperty("max_tokens") Integer maxTokens
    ) {
        private static GroqChatRequest fromPrompt(String model, String prompt) {
            return new GroqChatRequest(model, List.of(
                    new GroqChatMessage("system", "Responda somente JSON valido, sem markdown."),
                    new GroqChatMessage("user", prompt)
            ), 0.0, 120);
        }
    }

    private record GroqChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroqChatResponse(List<GroqChatChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GroqChatChoice(GroqChatMessage message) {
    }
}
