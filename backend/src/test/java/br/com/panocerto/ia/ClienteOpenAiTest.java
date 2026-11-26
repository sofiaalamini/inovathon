package br.com.panocerto.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Só testa a leitura do formato de resposta da OpenAI (sem chamar a internet). */
class ClienteOpenAiTest {

    @Test
    void juntaOTextoDaRespostaIgnorandoORaciocinio() throws Exception {
        var resposta = new ObjectMapper().readTree("""
                {"output":[{"type":"reasoning","summary":[]},
                           {"type":"message","content":[{"type":"output_text","text":"{\\"percevejos\\": 2}"}]}]}""");
        assertEquals("{\"percevejos\": 2}", ClienteOpenAi.textoDa(resposta));
    }
}
