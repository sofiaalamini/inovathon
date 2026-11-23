package br.com.panocerto.mip;

import br.com.panocerto.mip.RegraMip.Niveis;
import br.com.panocerto.mip.RegraMip.Ponto;
import br.com.panocerto.mip.RegraMip.Resultado;
import br.com.panocerto.modelo.Decisao;
import br.com.panocerto.modelo.Finalidade;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.com.panocerto.modelo.Decisao.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa todos os caminhos da regra do MIP: estádio, finalidade, V/R, PERTO, ACIMA e ABAIXO. */
class RegraMipTest {

    private final RegraMip regra = new RegraMip(Niveis.padrao());

    /** Três pontos iguais, para a média ser exatamente o valor informado. */
    private List<Ponto> tresPontos(int percevejos, int lagartas, double desfolha) {
        return List.of(new Ponto(percevejos, lagartas, desfolha),
                new Ponto(percevejos, lagartas, desfolha),
                new Ponto(percevejos, lagartas, desfolha));
    }

    private Decisao decidir(List<Ponto> pontos, String estadio, Finalidade finalidade) {
        return regra.decidir(pontos, estadio, finalidade).decisao();
    }

    // ---------- pontos mínimos ----------

    @Test
    void comMenosDeTresPontosFaltaDado() {
        Resultado r = regra.decidir(List.of(new Ponto(9, 9, 90), new Ponto(9, 9, 90)), "R5", Finalidade.GRAO);
        assertEquals(FALTA_DADO, r.decisao());
        assertTrue(r.mensagem().contains("Faltam 1"));
    }

    @Test
    void semNenhumPontoFaltamTres() {
        Resultado r = regra.decidir(List.of(), "R5", Finalidade.GRAO);
        assertEquals(FALTA_DADO, r.decisao());
        assertTrue(r.mensagem().contains("Faltam 3"));
    }

    // ---------- percevejo: antes e depois do R3 ----------

    @Test
    void percevejoAltoAntesDoR3EhIgnorado() {
        assertEquals(ABAIXO, decidir(tresPontos(10, 0, 0), "V5", Finalidade.GRAO));
        assertEquals(ABAIXO, decidir(tresPontos(10, 0, 0), "R1", Finalidade.GRAO));
        assertEquals(ABAIXO, decidir(tresPontos(10, 0, 0), "R2", Finalidade.GRAO));
    }

    @Test
    void percevejoContaAPartirDoR3() {
        assertEquals(ACIMA, decidir(tresPontos(2, 0, 0), "R3", Finalidade.GRAO));
        assertEquals(ACIMA, decidir(tresPontos(2, 0, 0), "R6", Finalidade.GRAO));
    }

    // ---------- percevejo: grão x semente ----------

    @Test
    void percevejoGraoTemNivelDois() {
        assertEquals(ACIMA, decidir(tresPontos(2, 0, 0), "R4", Finalidade.GRAO));
        assertEquals(ABAIXO, decidir(tresPontos(1, 0, 0), "R4", Finalidade.GRAO));
    }

    @Test
    void percevejoSementeTemNivelUm() {
        assertEquals(ACIMA, decidir(tresPontos(1, 0, 0), "R4", Finalidade.SEMENTE));
        assertEquals(ABAIXO, decidir(tresPontos(0, 0, 0), "R4", Finalidade.SEMENTE));
    }

    @Test
    void mediaDePercevejosUsaTodosOsPontos() {
        // média = (2 + 2 + 1) / 3 = 1,67 -> 83% do nível 2 -> PERTO
        List<Ponto> pontos = List.of(new Ponto(2, 0, 0), new Ponto(2, 0, 0), new Ponto(1, 0, 0));
        assertEquals(PERTO, decidir(pontos, "R4", Finalidade.GRAO));
    }

    @Test
    void umPontoAltoSozinhoNaoPassaOMesmoQueAMedia() {
        // média = (5 + 0 + 0) / 3 = 1,67 -> PERTO (não ACIMA)
        List<Ponto> pontos = List.of(new Ponto(5, 0, 0), new Ponto(0, 0, 0), new Ponto(0, 0, 0));
        assertEquals(PERTO, decidir(pontos, "R4", Finalidade.GRAO));
    }

    // ---------- lagarta ----------

    @Test
    void lagartaAcimaDoNivelVinte() {
        assertEquals(ACIMA, decidir(tresPontos(0, 20, 0), "V4", Finalidade.GRAO));
        assertEquals(ACIMA, decidir(tresPontos(0, 35, 0), "R4", Finalidade.GRAO));
    }

    @Test
    void lagartaPertoEntreSetentaPorCentoENivel() {
        assertEquals(PERTO, decidir(tresPontos(0, 14, 0), "V4", Finalidade.GRAO)); // 14 = 70% de 20
        assertEquals(PERTO, decidir(tresPontos(0, 19, 0), "V4", Finalidade.GRAO));
    }

    @Test
    void lagartaAbaixoDeSetentaPorCento() {
        assertEquals(ABAIXO, decidir(tresPontos(0, 13, 0), "V4", Finalidade.GRAO));
    }

    @Test
    void lagartaIgnoraOEstadio() {
        assertEquals(ACIMA, decidir(tresPontos(0, 20, 0), "R1", Finalidade.SEMENTE));
    }

    // ---------- desfolha: vegetativa (30%) x reprodutiva (15%) ----------

    @Test
    void desfolhaVegetativaTemNivelTrinta() {
        assertEquals(ACIMA, decidir(tresPontos(0, 0, 30), "V6", Finalidade.GRAO));
        assertEquals(PERTO, decidir(tresPontos(0, 0, 21), "V6", Finalidade.GRAO)); // 70% de 30
        assertEquals(ABAIXO, decidir(tresPontos(0, 0, 20), "V6", Finalidade.GRAO));
    }

    @Test
    void desfolhaReprodutivaTemNivelQuinze() {
        assertEquals(ACIMA, decidir(tresPontos(0, 0, 15), "R2", Finalidade.GRAO));
        assertEquals(PERTO, decidir(tresPontos(0, 0, 11), "R2", Finalidade.GRAO)); // 10,5 = 70% de 15
        assertEquals(ABAIXO, decidir(tresPontos(0, 0, 10), "R2", Finalidade.GRAO));
    }

    @Test
    void mesmaDesfolhaDecideDiferenteEmVEmR() {
        assertEquals(ABAIXO, decidir(tresPontos(0, 0, 15), "V8", Finalidade.GRAO));
        assertEquals(ACIMA, decidir(tresPontos(0, 0, 15), "R5", Finalidade.GRAO));
    }

    // ---------- resultado geral ----------

    @Test
    void tudoBaixoDaAbaixoEMensagemDeSeteDias() {
        Resultado r = regra.decidir(tresPontos(0, 1, 2), "R4", Finalidade.GRAO);
        assertEquals(ABAIXO, r.decisao());
        assertEquals(RegraMip.MSG_ABAIXO, r.mensagem());
    }

    @Test
    void perto_mandaVoltarEmTresDias() {
        Resultado r = regra.decidir(tresPontos(0, 15, 0), "R4", Finalidade.GRAO);
        assertEquals(PERTO, r.decisao());
        assertEquals(RegraMip.MSG_PERTO, r.mensagem());
    }

    @Test
    void acimaMandaAplicarSoNoTalhao() {
        Resultado r = regra.decidir(tresPontos(3, 0, 0), "R4", Finalidade.GRAO);
        assertEquals(ACIMA, r.decisao());
        assertEquals(RegraMip.MSG_ACIMA, r.mensagem());
    }

    @Test
    void acimaVenceQuandoUmaPragaEstaPertoEOutraAcima() {
        // percevejo PERTO (1,67), lagarta ACIMA (25) -> ACIMA
        List<Ponto> pontos = List.of(new Ponto(2, 25, 0), new Ponto(2, 25, 0), new Ponto(1, 25, 0));
        assertEquals(ACIMA, decidir(pontos, "R4", Finalidade.GRAO));
    }

    @Test
    void devolveMediasECalculaMotivos() {
        Resultado r = regra.decidir(tresPontos(2, 4, 10), "R4", Finalidade.GRAO);
        assertEquals(2.0, r.mediaPercevejos(), 0.001);
        assertEquals(4.0, r.mediaLagartas(), 0.001);
        assertEquals(10.0, r.mediaDesfolha(), 0.001);
        assertEquals(3, r.motivos().size());
    }

    @Test
    void niveisEditadosMudamADecisao() {
        // Se o agrônomo mudar o nível de lagarta para 10, 12 lagartas passam a ser ACIMA.
        RegraMip outra = new RegraMip(new Niveis(2, 1, 10, 30, 15, 70));
        assertEquals(ACIMA, outra.decidir(tresPontos(0, 12, 0), "V4", Finalidade.GRAO).decisao());
        assertEquals(ABAIXO, regra.decidir(tresPontos(0, 12, 0), "V4", Finalidade.GRAO).decisao());
    }

    // ---------- estádio ----------

    @Test
    void estadioValidoEReconhecido() {
        assertTrue(Estadio.valido("V1"));
        assertTrue(Estadio.valido("R8"));
        assertEquals(false, Estadio.valido("R9"));
        assertEquals(false, Estadio.valido("X3"));
        assertEquals(false, Estadio.valido(null));
    }
}
