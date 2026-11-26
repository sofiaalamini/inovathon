package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Fala com a OpenAI (GPT): lê a foto do pano e interpreta texto livre. */
@Component
public class ClienteOpenAi extends ClienteIaTexto {

    private final Configuracao config;
    private final RestClient http = clienteHttp("https://api.openai.com", 20);

    public ClienteOpenAi(Configuracao config, ObjectMapper json) {
        super(json);
        this.config = config;
    }

    @Override
    protected JsonNode perguntar(String pedido, byte[] imagem, String mime) {
        List<Map<String, Object>> conteudo = new ArrayList<>();
        conteudo.add(Map.of("type", "input_text", "text", pedido));
        if (imagem != null) {
            conteudo.add(Map.of("type", "input_image",
                    "image_url", "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(imagem)));
        }
        Map<String, Object> corpo = Map.of(
                "model", config.openaiModel(),
                "input", List.of(Map.of("role", "user", "content", conteudo)),
                "reasoning", Map.of("effort", "low"),
                "max_output_tokens", 2000);
        JsonNode resposta = http.post().uri("/v1/responses")
                .header("Authorization", "Bearer " + config.openaiApiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve().body(JsonNode.class);
        return lerJson(textoDa(resposta));
    }

    /** A resposta vem numa lista "output"; o texto está nos itens do tipo "output_text". */
    static String textoDa(JsonNode resposta) {
        StringBuilder texto = new StringBuilder();
        if (resposta != null) {
            for (JsonNode item : resposta.path("output")) {
                for (JsonNode parte : item.path("content")) {
                    if ("output_text".equals(parte.path("type").asText())) {
                        texto.append(parte.path("text").asText());
                    }
                }
            }
        }
        return texto.toString();
    }
}
