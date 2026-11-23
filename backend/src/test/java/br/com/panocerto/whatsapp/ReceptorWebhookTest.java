package br.com.panocerto.whatsapp;

import br.com.panocerto.robo.MensagemEntrada;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Conversão do formato da Meta para o formato do robô. */
class ReceptorWebhookTest {

    private final ObjectMapper json = new ObjectMapper();

    private MensagemEntrada converter(String mensagem) throws Exception {
        return ReceptorWebhook.converter(json.readTree(mensagem));
    }

    @Test
    void texto() throws Exception {
        var m = converter("{\"from\":\"5545999990001\",\"id\":\"w1\",\"type\":\"text\",\"text\":{\"body\":\"2 percevejos\"}}");
        assertEquals(MensagemEntrada.Tipo.TEXTO, m.tipo());
        assertEquals("2 percevejos", m.texto());
        assertEquals("w1", m.idExterno());
    }

    @Test
    void foto() throws Exception {
        var m = converter("{\"from\":\"1\",\"id\":\"w2\",\"type\":\"image\",\"image\":{\"id\":\"MEDIA1\",\"mime_type\":\"image/jpeg\"}}");
        assertEquals(MensagemEntrada.Tipo.IMAGEM, m.tipo());
        assertEquals("MEDIA1", m.idMidia());
    }

    @Test
    void audio() throws Exception {
        var m = converter("{\"from\":\"1\",\"id\":\"w3\",\"type\":\"audio\",\"audio\":{\"id\":\"MEDIA2\"}}");
        assertEquals(MensagemEntrada.Tipo.AUDIO, m.tipo());
        assertEquals("MEDIA2", m.idMidia());
    }

    @Test
    void respostaDeBotao() throws Exception {
        var m = converter("{\"from\":\"1\",\"id\":\"w4\",\"type\":\"interactive\",\"interactive\":"
                + "{\"type\":\"button_reply\",\"button_reply\":{\"id\":\"DESF_10\",\"title\":\"10%\"}}}");
        assertEquals(MensagemEntrada.Tipo.BOTAO, m.tipo());
        assertEquals("DESF_10", m.botaoId());
        assertEquals("10%", m.texto());
    }

    @Test
    void respostaDeLista() throws Exception {
        var m = converter("{\"from\":\"1\",\"id\":\"w5\",\"type\":\"interactive\",\"interactive\":"
                + "{\"type\":\"list_reply\",\"list_reply\":{\"id\":\"TALHAO_7\",\"title\":\"Sede\"}}}");
        assertEquals("TALHAO_7", m.botaoId());
    }

    @Test
    void tipoDesconhecidoViraOutro() throws Exception {
        var m = converter("{\"from\":\"1\",\"id\":\"w6\",\"type\":\"sticker\"}");
        assertEquals(MensagemEntrada.Tipo.OUTRO, m.tipo());
    }
}
