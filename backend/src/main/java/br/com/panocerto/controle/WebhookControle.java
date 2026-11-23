package br.com.panocerto.controle;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.whatsapp.ReceptorWebhook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Endereço que a Meta chama. GET faz a verificação inicial; POST entrega as mensagens. */
@RestController
@RequestMapping("/webhook/whatsapp")
public class WebhookControle {

    private static final Logger log = LoggerFactory.getLogger(WebhookControle.class);

    private final Configuracao config;
    private final ReceptorWebhook receptor;
    private final ObjectMapper json;

    public WebhookControle(Configuracao config, ReceptorWebhook receptor, ObjectMapper json) {
        this.config = config;
        this.receptor = receptor;
        this.json = json;
    }

    /** A Meta confere o endereço mandando hub.mode, hub.verify_token e hub.challenge. */
    @GetMapping
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
                                            @RequestParam(name = "hub.verify_token", required = false) String token,
                                            @RequestParam(name = "hub.challenge", required = false) String desafio) {
        String esperado = config.whatsappVerifyToken();
        if ("subscribe".equals(modo) && esperado != null && !esperado.isBlank() && desafio != null
                && iguais(esperado, token)) {
            return ResponseEntity.ok(desafio);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Confere a assinatura X-Hub-Signature-256 (HMAC-SHA256 do corpo com o APP_SECRET).
     * Depois responde 200 na hora e deixa o robô trabalhar em segundo plano.
     */
    @PostMapping
    public ResponseEntity<Void> receber(@RequestBody byte[] corpo,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
        if (!assinaturaValida(corpo, assinatura)) {
            log.warn("Webhook recusado: assinatura ausente ou inválida.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            JsonNode payload = json.readTree(corpo);
            receptor.processar(payload);
        } catch (Exception e) {
            // A Meta repete a entrega se não receber 200; por isso nunca devolvemos erro aqui.
            log.error("Webhook com corpo inválido: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    boolean assinaturaValida(byte[] corpo, String assinatura) {
        String segredo = config.whatsappAppSecret();
        if (segredo == null || segredo.isBlank() || assinatura == null || !assinatura.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String esperada = "sha256=" + HexFormat.of().formatHex(mac.doFinal(corpo));
            return iguais(esperada, assinatura.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }

    /** Comparação em tempo constante, para não vazar o segredo pelo tempo de resposta. */
    private boolean iguais(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
