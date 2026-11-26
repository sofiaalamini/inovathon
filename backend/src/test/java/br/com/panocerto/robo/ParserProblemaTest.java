package br.com.panocerto.robo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Números impossíveis são recusados, e "1000" não vira "0". */
class ParserProblemaTest {

    @Test
    void recusaNegativoGrandeDemaisEPorcentagemAcimaDe100() {
        assertNotNull(ParserTexto.problema("-3 percevejos 2 lagartas"));
        assertNotNull(ParserTexto.problema("1000 percevejos 5 lagartas 10%"));
        assertNotNull(ParserTexto.problema("2 percevejos 3 lagartas 150%"));
        assertNull(ParserTexto.problema("999 percevejos 3 lagartas 100%"));
        assertNull(ParserTexto.problema("dois percevejos e tres lagartas 10%"));
    }

    @Test
    void milNaoViraZero() {
        assertNull(ParserTexto.interpretar("1000 percevejos").percevejos());
    }

    @Test
    void soNumeroEPragaEhSimples() {
        assertTrue(ParserTexto.simples("2 percevejos 3 lagartas 10%"));
        assertTrue(ParserTexto.simples("Percevejos: 2, lagartas: 3, desfolha 10%"));
        assertFalse(ParserTexto.simples("nenhum percevejo, umas trinta lagartinha pequena e só uma grande"));
        assertFalse(ParserTexto.simples("bati o pano, deu dois percevejo"));
    }
}
