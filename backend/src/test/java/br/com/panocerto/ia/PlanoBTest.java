package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Com o plano B ligado, a IA não é chamada mesmo havendo chave. */
class PlanoBTest {

    private final Configuracao config = new Configuracao("x".repeat(40), 1, "", "m", "chave-de-teste", "", "", "", "",
            "", 0, "", false, "", "modelo", "modelo-audio", false, "");

    @Test
    void respondeComDemonstracaoSemChamarAInternet() {
        var json = new ObjectMapper();
        var ia = new ServicoIa(config, new ClienteClaude(config, json), new ClienteOpenAi(config, json),
                new ClienteTranscricao(config));
        ia.definirPlanoB(true);

        assertTrue(ia.contarInsetos(new byte[]{1, 2, 3}, "image/jpeg").simulada());
        assertTrue(ia.transcrever(new byte[]{1, 2, 3}, "audio/webm", null).simulada());
        assertTrue(ia.emDemonstracao());
        // com a fala que o navegador entendeu, a demonstração usa o que a pessoa disse
        assertEquals("contei 4 percevejos", ia.transcrever(new byte[]{1}, "audio/webm", "contei 4 percevejos").texto());
    }
}
