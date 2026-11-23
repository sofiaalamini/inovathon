package br.com.panocerto.whatsapp;

import br.com.panocerto.robo.MensagemEntrada;
import br.com.panocerto.robo.ServicoRobo;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Traduz o JSON da Meta em MensagemEntrada e entrega ao robô.
 * Roda em segundo plano (@Async), para o webhook responder 200 logo.
 */
@Service
public class ReceptorWebhook {

    private static final Logger log = LoggerFactory.getLogger(ReceptorWebhook.class);

    private final ServicoRobo robo;

    public ReceptorWebhook(ServicoRobo robo) {
        this.robo = robo;
    }

    @Async("filaDoRobo")
    public void processar(JsonNode corpo) {
        for (JsonNode entrada : corpo.path("entry")) {
            for (JsonNode mudanca : entrada.path("changes")) {
                for (JsonNode mensagem : mudanca.path("value").path("messages")) {
                    try {
                        robo.processar(converter(mensagem));
                    } catch (Exception e) {
                        // Um erro numa mensagem não pode impedir as outras.
                        log.error("Erro ao processar mensagem do webhook", e);
                    }
                }
            }
        }
    }

    /** Converte uma mensagem no formato da Meta para o formato simples do robô. */
    static MensagemEntrada converter(JsonNode m) {
        String de = m.path("from").asText();
        String id = m.path("id").asText(null);
        String tipo = m.path("type").asText();
        return switch (tipo) {
            case "text" -> new MensagemEntrada(de, MensagemEntrada.Tipo.TEXTO, m.path("text").path("body").asText(),
                    null, null, null, id);
            case "image" -> new MensagemEntrada(de, MensagemEntrada.Tipo.IMAGEM, m.path("image").path("caption").asText(null),
                    null, m.path("image").path("id").asText(), null, id);
            case "audio" -> new MensagemEntrada(de, MensagemEntrada.Tipo.AUDIO, null,
                    null, m.path("audio").path("id").asText(), null, id);
            case "interactive" -> {
                JsonNode inter = m.path("interactive");
                JsonNode resposta = inter.path("type").asText().equals("list_reply")
                        ? inter.path("list_reply") : inter.path("button_reply");
                yield new MensagemEntrada(de, MensagemEntrada.Tipo.BOTAO, resposta.path("title").asText(),
                        resposta.path("id").asText(), null, null, id);
            }
            // Resposta rápida de um template (ex.: botão do lembrete): trata como texto.
            case "button" -> new MensagemEntrada(de, MensagemEntrada.Tipo.TEXTO, m.path("button").path("text").asText(),
                    null, null, null, id);
            default -> new MensagemEntrada(de, MensagemEntrada.Tipo.OUTRO, null, null, null, null, id);
        };
    }
}
