package br.com.panocerto.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Sem o produtor falar de folha, a desfolha que a IA devolver é ignorada (o robô pergunta). */
class DesfolhaTest {

    private final ClienteIaTexto ia = new ClienteIaTexto(new ObjectMapper()) {
        @Override
        protected JsonNode perguntar(String pedido, byte[] imagem, String mime) {
            return lerJson("{\"percevejos\": 4, \"lagartas\": 2, \"desfolha\": 10}");
        }
    };

    @Test
    void ignoraDesfolhaQueOProdutorNaoFalou() {
        assertNull(ia.extrairNumeros("contei quatro percevejos e duas lagartas").desfolha());
    }

    @Test
    void usaDesfolhaQuandoOProdutorFalou() {
        assertEquals(10, ia.extrairNumeros("quatro percevejos, duas lagartas, folha uns dez por cento").desfolha());
    }
}
