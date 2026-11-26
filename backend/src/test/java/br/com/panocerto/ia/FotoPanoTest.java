package br.com.panocerto.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lê o campo "pano" da resposta da IA: foto que não é pano x pano vazio. */
class FotoPanoTest {

    private static ClienteIaTexto respondendo(String json) {
        return new ClienteIaTexto(new ObjectMapper()) {
            @Override
            protected JsonNode perguntar(String pedido, byte[] imagem, String mime) {
                return lerJson(json);
            }
        };
    }

    @Test
    void fotoQueNaoEhPano() {
        assertFalse(respondendo("{\"pano\": false, \"percevejos\": 0, \"lagartas\": 0, \"confianca\": 0.9}")
                .contarFoto(new byte[]{1}, "image/jpeg").pano());
    }

    @Test
    void panoVazioEhPanoComZero() {
        var c = respondendo("{\"pano\": true, \"percevejos\": 0, \"lagartas\": 0, \"confianca\": 0.9}")
                .contarFoto(new byte[]{1}, "image/jpeg");
        assertTrue(c.pano());
        assertEquals(0, c.percevejos() + c.lagartas());
    }
}
