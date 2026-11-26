package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Fala com a API da Anthropic (Claude): lê a foto do pano e interpreta texto livre. */
@Component
public class ClienteClaude extends ClienteIaTexto {

    private final Configuracao config;
    private final RestClient http = clienteHttp("https://api.anthropic.com", 20);

    public ClienteClaude(Configuracao config, ObjectMapper json) {
        super(json);
        this.config = config;
    }

    @Override
    protected JsonNode perguntar(String pedido, byte[] imagem, String mime) {
        Map<String, Object> texto = Map.of("type", "text", "text", pedido);
        List<Map<String, Object>> conteudo = imagem == null ? List.of(texto) : List.of(
                Map.of("type", "image", "source", Map.of(
                        "type", "base64", "media_type", mime, "data", Base64.getEncoder().encodeToString(imagem))),
                texto);
        Map<String, Object> corpo = Map.of(
                "model", config.anthropicModel(),
                "max_tokens", 200,
                "messages", List.of(Map.of("role", "user", "content", conteudo)));
        JsonNode resposta = http.post().uri("/v1/messages")
                .header("x-api-key", config.anthropicApiKey())
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve().body(JsonNode.class);
        return lerJson(resposta == null ? "" : resposta.path("content").path(0).path("text").asText(""));
    }
}
