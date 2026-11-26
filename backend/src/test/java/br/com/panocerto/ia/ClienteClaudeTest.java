package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Só testa a leitura da resposta do Claude (sem chamar a internet). */
class ClienteClaudeTest {

    private final ClienteClaude claude = new ClienteClaude(
            new Configuracao("x".repeat(40), 1, "", "claude-sonnet-5-5", "", "", "", "", "", "", 0, "", false, "", "", "", false, ""),
            new ObjectMapper());

    @Test
    void leJsonPuro() {
        var no = claude.lerJson("{\"percevejos\": 2, \"lagartas\": 3, \"confianca\": 0.8}");
        assertEquals(2, no.get("percevejos").asInt());
        assertEquals(0.8, no.get("confianca").asDouble(), 0.001);
    }

    @Test
    void leJsonEmbrulhadoEmBlocoDeCodigo() {
        var no = claude.lerJson("```json\n{\"percevejos\": 1, \"lagartas\": 0, \"confianca\": 0.5}\n```");
        assertEquals(1, no.get("percevejos").asInt());
    }

    @Test
    void respostaSemJsonEhErro() {
        assertThrows(IllegalStateException.class, () -> claude.lerJson("Não consegui contar."));
    }

    @Test
    void semChavesAIaNaoEstaAtiva() {
        var config = new Configuracao("x".repeat(40), 1, "", "m", "", "", "", "", "", "", 0, "", false, "", "", "", false, "");
        assertFalse(config.claudeConfigurado());
        assertFalse(config.openaiConfigurado());
        assertFalse(config.whatsappConfigurado());
    }
}
