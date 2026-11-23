package br.com.panocerto.whatsapp;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mensageiro real: WhatsApp Cloud API oficial da Meta (Graph API). */
public class MensageiroCloudApi implements MensageiroWhatsApp {

    private static final String VERSAO = "v21.0";
    private static final long LIMITE_MIDIA = 20L * 1024 * 1024;

    private final Configuracao config;
    private final RestClient http;

    public MensageiroCloudApi(Configuracao config) {
        this.config = config;
        var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build());
        fabrica.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().requestFactory(fabrica)
                .defaultHeader("Authorization", "Bearer " + config.whatsappToken()).build();
    }

    @Override
    public void enviarTexto(String telefone, String texto) {
        enviar(Map.of("messaging_product", "whatsapp", "to", telefone, "type", "text",
                "text", Map.of("body", cortar(texto, 4000))));
    }

    @Override
    public void enviarBotoes(String telefone, String texto, List<Opcao> botoes) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Opcao botao : botoes.stream().limit(3).toList()) {
            lista.add(Map.of("type", "reply", "reply", Map.of("id", botao.id(), "title", cortar(botao.titulo(), 20))));
        }
        enviar(Map.of("messaging_product", "whatsapp", "to", telefone, "type", "interactive",
                "interactive", Map.of("type", "button", "body", Map.of("text", cortar(texto, 1024)),
                        "action", Map.of("buttons", lista))));
    }

    @Override
    public void enviarLista(String telefone, String texto, String tituloDoBotao, List<Opcao> itens) {
        List<Map<String, Object>> linhas = new ArrayList<>();
        for (Opcao item : itens.stream().limit(10).toList()) {
            linhas.add(Map.of("id", item.id(), "title", cortar(item.titulo(), 24)));
        }
        enviar(Map.of("messaging_product", "whatsapp", "to", telefone, "type", "interactive",
                "interactive", Map.of("type", "list", "body", Map.of("text", cortar(texto, 1024)),
                        "action", Map.of("button", cortar(tituloDoBotao, 20),
                                "sections", List.of(Map.of("title", "Talhões", "rows", linhas))))));
    }

    @Override
    public void enviarTemplate(String telefone, String nomeDoTemplate, List<String> parametros) {
        List<Map<String, String>> variaveis = parametros.stream().map(p -> Map.of("type", "text", "text", p)).toList();
        enviar(Map.of("messaging_product", "whatsapp", "to", telefone, "type", "template",
                "template", Map.of("name", nomeDoTemplate, "language", Map.of("code", "pt_BR"),
                        "components", List.of(Map.of("type", "body", "parameters", variaveis)))));
    }

    @Override
    public Midia baixarMidia(String idDaMidia) {
        // Passo 1: a Meta informa a URL temporária e o tipo da mídia.
        JsonNode info = http.get().uri("https://graph.facebook.com/" + VERSAO + "/" + idDaMidia)
                .retrieve().body(JsonNode.class);
        String url = info == null ? "" : info.path("url").asText("");
        String mime = info == null ? "application/octet-stream" : info.path("mime_type").asText("application/octet-stream");
        if (!urlDaMetaConfiavel(url)) {
            throw new IllegalStateException("URL de mídia inesperada.");
        }
        // Passo 2: baixa os bytes (também com o token).
        byte[] bytes = http.get().uri(URI.create(url)).retrieve().body(byte[].class);
        if (bytes == null || bytes.length == 0 || bytes.length > LIMITE_MIDIA) {
            throw new IllegalStateException("Mídia vazia ou grande demais.");
        }
        return new Midia(bytes, mime);
    }

    @Override
    public String modo() {
        return "cloud-api";
    }

    private void enviar(Map<String, Object> corpo) {
        http.post().uri("https://graph.facebook.com/" + VERSAO + "/" + config.whatsappPhoneNumberId() + "/messages")
                .contentType(MediaType.APPLICATION_JSON).body(corpo).retrieve().toBodilessEntity();
    }

    /** Só baixa de endereços https da Meta, para ninguém usar o robô para acessar outros sites. */
    private boolean urlDaMetaConfiavel(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            return "https".equals(uri.getScheme())
                    && (host.endsWith(".fbsbx.com") || host.endsWith(".facebook.com") || host.endsWith(".fbcdn.net")
                    || host.endsWith(".whatsapp.net") || host.endsWith(".whatsapp.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String cortar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + "…";
    }
}
