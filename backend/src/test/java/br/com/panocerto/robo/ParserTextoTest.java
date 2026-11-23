package br.com.panocerto.robo;

import br.com.panocerto.robo.ParserTexto.Numeros;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTextoTest {

    @Test
    void textoCompletoComTudo() {
        Numeros n = ParserTexto.interpretar("2 percevejos 3 lagartas 10%");
        assertEquals(2, n.percevejos());
        assertEquals(3, n.lagartas());
        assertEquals(10, n.desfolha());
    }

    @Test
    void semDesfolhaDeixaNulo() {
        Numeros n = ParserTexto.interpretar("2 percevejos 3 lagartas");
        assertEquals(2, n.percevejos());
        assertEquals(3, n.lagartas());
        assertNull(n.desfolha());
    }

    @Test
    void faladoDoCampoComNumeroPorExtenso() {
        Numeros n = ParserTexto.interpretar("bati três vez, deu dois percevejo e umas quatro lagarta");
        assertEquals(2, n.percevejos());
        assertEquals(4, n.lagartas());
        assertNull(n.desfolha());
    }

    @Test
    void maiusculasAcentosEPontuacao() {
        Numeros n = ParserTexto.interpretar("Deu 1 Percevejo, DUAS lagartas. Desfolha: 5");
        assertEquals(1, n.percevejos());
        assertEquals(2, n.lagartas());
        assertEquals(5, n.desfolha());
    }

    @Test
    void numeroPorExtensoMaiorNaoViraOMenor() {
        Numeros n = ParserTexto.interpretar("dezesseis lagartas e dez por cento");
        assertEquals(16, n.lagartas());
        assertEquals(10, n.desfolha());
    }

    @Test
    void nenhumInsetoViraZero() {
        Numeros n = ParserTexto.interpretar("nenhum percevejo e 4 lagartas");
        assertEquals(0, n.percevejos());
        assertEquals(4, n.lagartas());
    }

    @Test
    void falouDeUmInsetoSoOOutroEhZero() {
        Numeros n = ParserTexto.interpretar("3 percevejos");
        assertEquals(3, n.percevejos());
        assertEquals(0, n.lagartas());
        assertTrue(n.temContagem());
    }

    @Test
    void formaComDoisPontos() {
        Numeros n = ParserTexto.interpretar("percevejos: 2 lagartas: 5");
        assertEquals(2, n.percevejos());
        assertEquals(5, n.lagartas());
    }

    @Test
    void textoSemNumerosDeInsetoNaoTemContagem() {
        assertFalse(ParserTexto.interpretar("bom dia, tudo bem?").temContagem());
        assertFalse(ParserTexto.interpretar("bati três vez hoje").temContagem());
        assertFalse(ParserTexto.interpretar(null).temContagem());
    }

    @Test
    void desfolhaAcimaDeCemEhIgnorada() {
        assertNull(ParserTexto.interpretar("2 percevejos 150%").desfolha());
    }

    @Test
    void respostaCurtaDeDesfolha() {
        assertEquals(10, ParserTexto.interpretarDesfolha("10"));
        assertEquals(10, ParserTexto.interpretarDesfolha("10%"));
        assertEquals(20, ParserTexto.interpretarDesfolha("20%+"));
        assertEquals(10, ParserTexto.interpretarDesfolha("dez por cento"));
        assertEquals(0, ParserTexto.interpretarDesfolha("nenhuma"));
    }

    @Test
    void respostaCurtaDeDesfolhaRejeitaTextoQueNaoEhPorcentagem() {
        assertNull(ParserTexto.interpretarDesfolha("2 percevejos 3 lagartas"));
        assertNull(ParserTexto.interpretarDesfolha("10 e 20"));
        assertNull(ParserTexto.interpretarDesfolha("150"));
        assertNull(ParserTexto.interpretarDesfolha("não sei"));
    }

    @Test
    void comandosSaoNormalizados() {
        assertEquals("apliquei", ParserTexto.comando("  Apliquei! "));
        assertEquals("ajuda", ParserTexto.comando("AJUDA"));
    }
}
