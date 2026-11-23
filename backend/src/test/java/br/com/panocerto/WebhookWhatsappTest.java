package br.com.panocerto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Webhook da Meta: verificação, assinatura, resposta 200 e processamento em segundo plano. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookWhatsappTest {

    private static final String SEGREDO = "segredo-de-teste";   // application-test.yml

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String assinar(String corpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8)));
    }

    private String mensagemDeTexto(String de, String id, String texto) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
                  "messaging_product":"whatsapp","messages":[{"from":"%s","id":"%s","type":"text","text":{"body":"%s"}}]}}]}]}
                """.formatted(de, id, texto);
    }

    private String tokenDoPainel() throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"tecnico@panocerto.com\",\"senha\":\"123456\"}")).andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("token").asText();
    }

    /** Espera o processamento em segundo plano e devolve as respostas do robô para o produtor. */
    private int respostasDoRobo(String token, String nomeDoProdutor, String contendo) throws Exception {
        long id = -1;
        for (JsonNode p : json.readTree(mvc.perform(get("/api/conversas").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())) {
            if (p.get("nome").asText().equals(nomeDoProdutor)) id = p.get("produtorId").asLong();
        }
        for (int tentativa = 0; tentativa < 50; tentativa++) {
            int achadas = 0;
            for (JsonNode m : json.readTree(mvc.perform(get("/api/conversas/" + id + "/mensagens")
                    .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString())) {
                if (m.get("direcao").asText().equals("SAIDA") && m.path("texto").asText().contains(contendo)) achadas++;
            }
            if (achadas > 0 || tentativa == 49) return achadas;
            Thread.sleep(100);
        }
        return 0;
    }

    @Test
    void verificacaoDaMetaDevolveODesafio() throws Exception {
        mvc.perform(get("/webhook/whatsapp").param("hub.mode", "subscribe")
                        .param("hub.verify_token", "token-de-teste").param("hub.challenge", "12345"))
                .andExpect(status().isOk()).andExpect(content().string("12345"));
    }

    @Test
    void verificacaoComTokenErradoEhRecusada() throws Exception {
        mvc.perform(get("/webhook/whatsapp").param("hub.mode", "subscribe")
                        .param("hub.verify_token", "errado").param("hub.challenge", "12345"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postSemAssinaturaOuComAssinaturaFalsaEhRecusado() throws Exception {
        String corpo = mensagemDeTexto("5545999990004", "wamid.A1", "ajuda");
        mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo))
                .andExpect(status().isForbidden());
        mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo)
                        .header("X-Hub-Signature-256", "sha256=" + "0".repeat(64)))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAssinadoResponde200EORoboRespondeEmSegundoPlano() throws Exception {
        String corpo = mensagemDeTexto("5545999990004", "wamid.B1", "ajuda");
        mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo)
                        .header("X-Hub-Signature-256", assinar(corpo)))
                .andExpect(status().isOk());
        assertTrue(respostasDoRobo(tokenDoPainel(), "Sandra", "Como usar o PanoCerto") >= 1);
    }

    @Test
    void mensagemRepetidaPelaMetaNaoEhProcessadaDuasVezes() throws Exception {
        String corpo = mensagemDeTexto("5545999990005", "wamid.C1", "ajuda");
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo)
                    .header("X-Hub-Signature-256", assinar(corpo))).andExpect(status().isOk());
        }
        String token = tokenDoPainel();
        assertTrue(respostasDoRobo(token, "Ivo", "Como usar o PanoCerto") >= 1);
        Thread.sleep(500);
        assertEquals(1, respostasDoRobo(token, "Ivo", "Como usar o PanoCerto"));
    }

    @Test
    void corpoInvalidoComAssinaturaCertaAindaResponde200() throws Exception {
        String corpo = "isto não é json";
        mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo)
                        .header("X-Hub-Signature-256", assinar(corpo)))
                .andExpect(status().isOk());
    }

    @Test
    void numeroSemOEscritoNonoDigitoAindaEhReconhecido() throws Exception {
        // A Meta pode mandar 554599990001 (sem o 9) para o produtor cadastrado como 5545999990001.
        String corpo = mensagemDeTexto("554599990001", "wamid.D1", "ajuda");
        mvc.perform(post("/webhook/whatsapp").contentType("application/json").content(corpo)
                .header("X-Hub-Signature-256", assinar(corpo))).andExpect(status().isOk());
        assertTrue(respostasDoRobo(tokenDoPainel(), "Valdir", "Como usar o PanoCerto") >= 1);
    }
}
