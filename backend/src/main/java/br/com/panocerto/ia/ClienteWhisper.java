package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/** Transcreve áudio com o Whisper da OpenAI (o Claude não transcreve áudio). Idioma fixo: português. */
@Component
public class ClienteWhisper {

    private final Configuracao config;
    private final RestClient http;

    public ClienteWhisper(Configuracao config) {
        this.config = config;
        var fabrica = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(90));
        this.http = RestClient.builder().baseUrl("https://api.openai.com").requestFactory(fabrica).build();
    }

    public String transcrever(byte[] audio, String tipoMime) {
        String nomeDoArquivo = "audio." + extensao(tipoMime);
        // O Whisper decide o formato pelo nome do arquivo, por isso o recurso informa um nome.
        ByteArrayResource arquivo = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return nomeDoArquivo;
            }
        };
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", arquivo);
        corpo.add("model", "whisper-1");
        corpo.add("language", "pt");

        JsonNode resposta = http.post().uri("/v1/audio/transcriptions")
                .header("Authorization", "Bearer " + config.openaiApiKey())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(corpo)
                .retrieve().body(JsonNode.class);
        String texto = resposta == null ? "" : resposta.path("text").asText("").trim();
        if (texto.isEmpty()) {
            throw new IllegalStateException("O Whisper não entendeu o áudio.");
        }
        return texto;
    }

    private String extensao(String tipoMime) {
        String mime = tipoMime == null ? "" : tipoMime.toLowerCase();
        if (mime.contains("ogg") || mime.contains("opus")) return "ogg";
        if (mime.contains("webm")) return "webm";
        if (mime.contains("mpeg") || mime.contains("mp3")) return "mp3";
        if (mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac")) return "m4a";
        if (mime.contains("wav")) return "wav";
        return "ogg";
    }
}
