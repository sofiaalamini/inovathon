package br.com.panocerto;

import br.com.panocerto.controle.ExportacaoControle;
import br.com.panocerto.modelo.Cooperativa;
import br.com.panocerto.modelo.Papel;
import br.com.panocerto.modelo.Usuario;
import br.com.panocerto.repositorio.RepositorioCooperativa;
import br.com.panocerto.repositorio.RepositorioUsuario;
import br.com.panocerto.servico.ServicoLembrete;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** API do painel: dashboard, alertas, cadastros, regras, CSV, lembretes e isolamento entre cooperativas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PainelApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ServicoLembrete lembrete;
    @Autowired RepositorioCooperativa cooperativas;
    @Autowired RepositorioUsuario usuarios;
    @Autowired PasswordEncoder encoder;

    private String token;

    @BeforeEach
    void entrar() throws Exception {
        token = login("tecnico@panocerto.com", "123456");
    }

    private String login(String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}")).andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("token").asText();
    }

    private JsonNode obter(String url) throws Exception {
        return json.readTree(mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", "Bearer " + token).contentType("application/json");
    }

    private JsonNode talhaoPorNome(String nome) throws Exception {
        for (JsonNode t : obter("/api/talhoes")) {
            if (t.get("nome").asText().equals(nome)) return t;
        }
        throw new AssertionError("Talhão não encontrado: " + nome);
    }

    // ---------------------------------------------------------------- dashboard

    @Test
    void dashboardAbreComDadosDeExemplo() throws Exception {
        JsonNode d = obter("/api/dashboard");
        assertTrue(d.get("acima").size() >= 2, "Sede e Várzea devem estar ACIMA");
        assertTrue(d.get("perto").size() >= 2);
        assertTrue(d.get("semMonitoramento").size() >= 2, "Lote 3 e Sítio Boa Vista estão parados há 7+ dias");
        assertEquals(12, d.get("pressaoSemanal").size());
        assertTrue(d.get("aplicacoesEvitadas").asDouble() > 0);
        assertTrue(d.get("economiaEstimada").asDouble() > 0);
        assertEquals(36.0, d.get("aplicacoesCalendario").asDouble(), 0.001);   // 10 talhões x 3,6
        // quem está há mais tempo sem monitorar vem primeiro
        assertTrue(d.get("semMonitoramento").get(0).get("diasSemMonitorar").asInt()
                >= d.get("semMonitoramento").get(1).get("diasSemMonitorar").asInt());
    }

    // ---------------------------------------------------------------- alertas

    @Test
    void tecnicoConfirmaAlertaEOProdutorRecebeNoWhatsApp() throws Exception {
        long alertaId = alertaPendenteDe("Sede");
        mvc.perform(autenticado(post("/api/alertas/" + alertaId + "/confirmar"))).andExpect(status().isOk());

        JsonNode produtor = produtorPorNome("Valdir");
        JsonNode mensagens = obter("/api/conversas/" + produtor.get("produtorId").asLong() + "/mensagens");
        boolean avisou = false;
        for (JsonNode m : mensagens) {
            if (m.path("texto").asText().contains("O técnico confirmou: aplique só no talhão Sede")) avisou = true;
        }
        assertTrue(avisou);
        // confirmar de novo é conflito
        mvc.perform(autenticado(post("/api/alertas/" + alertaId + "/confirmar"))).andExpect(status().isConflict());
        // o talhão sai da lista de ACIMA
        for (JsonNode t : obter("/api/dashboard").get("acima")) {
            assertNotEquals("Sede", t.get("nome").asText());
        }
    }

    @Test
    void tecnicoPedeParaRevisitar() throws Exception {
        long alertaId = alertaPendenteDe("Várzea");
        mvc.perform(autenticado(post("/api/alertas/" + alertaId + "/revisitar"))).andExpect(status().isOk());
        mvc.perform(autenticado(post("/api/alertas/" + alertaId + "/revisitar"))).andExpect(status().isConflict());
    }

    private long alertaPendenteDe(String talhao) throws Exception {
        for (JsonNode a : obter("/api/alertas")) {
            if (a.get("talhaoNome").asText().equals(talhao) && a.get("status").asText().equals("PENDENTE")) {
                return a.get("id").asLong();
            }
        }
        throw new AssertionError("Sem alerta pendente em " + talhao);
    }

    private JsonNode produtorPorNome(String nome) throws Exception {
        for (JsonNode p : obter("/api/conversas")) {
            if (p.get("nome").asText().equals(nome)) return p;
        }
        throw new AssertionError(nome);
    }

    // ---------------------------------------------------------------- cadastros

    @Test
    void crudDeProdutorComValidacaoDeTelefone() throws Exception {
        mvc.perform(autenticado(post("/api/produtores")).content("{\"nome\":\"Teste\",\"telefone\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/produtores")).content("{\"nome\":\"\",\"telefone\":\"+55 45 99999-0100\"}"))
                .andExpect(status().isBadRequest());

        String criado = mvc.perform(autenticado(post("/api/produtores"))
                        .content("{\"nome\":\"Novo Produtor\",\"telefone\":\"+55 45 99999-0100\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode p = json.readTree(criado);
        assertEquals("5545999990100", p.get("telefone").asText());   // só dígitos

        // telefone repetido (inclusive sem o 9)
        mvc.perform(autenticado(post("/api/produtores")).content("{\"nome\":\"Outro\",\"telefone\":\"5545999990100\"}"))
                .andExpect(status().isConflict());
        mvc.perform(autenticado(post("/api/produtores")).content("{\"nome\":\"Outro\",\"telefone\":\"554599990100\"}"))
                .andExpect(status().isConflict());

        mvc.perform(autenticado(put("/api/produtores/" + p.get("id").asLong()))
                .content("{\"nome\":\"Nome Mudado\",\"telefone\":\"5545999990100\"}")).andExpect(status().isOk());
        mvc.perform(autenticado(delete("/api/produtores/" + p.get("id").asLong()))).andExpect(status().isNoContent());
    }

    @Test
    void crudDeTalhaoValidaEstadioEProtegeHistorico() throws Exception {
        long produtorId = produtorPorNome("Ivo").get("produtorId").asLong();
        String base = "\"hectares\":10,\"finalidade\":\"GRAO\",\"produtorId\":" + produtorId;

        mvc.perform(autenticado(post("/api/talhoes")).content("{\"nome\":\"X\",\"estadio\":\"R9\"," + base + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/talhoes")).content("{\"nome\":\"X\",\"estadio\":\"R3\",\"hectares\":0,\"finalidade\":\"GRAO\",\"produtorId\":" + produtorId + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/talhoes")).content("{\"nome\":\"X\",\"estadio\":\"R3\",\"latitude\":-24.9," + base + "}"))
                .andExpect(status().isBadRequest());   // latitude sem longitude

        String criado = mvc.perform(autenticado(post("/api/talhoes"))
                        .content("{\"nome\":\"Talhão Novo\",\"estadio\":\"V3\",\"latitude\":-24.95,\"longitude\":-53.45," + base + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(criado).get("id").asLong();
        mvc.perform(autenticado(delete("/api/talhoes/" + id))).andExpect(status().isNoContent());   // sem histórico: pode

        // talhão com histórico não pode ser excluído
        long comHistorico = talhaoPorNome("Fazenda Nova").get("id").asLong();
        mvc.perform(autenticado(delete("/api/talhoes/" + comHistorico))).andExpect(status().isConflict());
    }

    @Test
    void detalheDoTalhaoTrazHistoricoComAmostras() throws Exception {
        long id = talhaoPorNome("Sede").get("id").asLong();
        JsonNode d = obter("/api/talhoes/" + id);
        assertTrue(d.get("monitoramentos").size() >= 6);
        JsonNode primeiro = d.get("monitoramentos").get(0);
        assertEquals(3, primeiro.get("amostras").size());
        assertNotNull(primeiro.get("recomendacao"));
        assertTrue(d.get("aplicacoes").size() >= 1);
    }

    // ---------------------------------------------------------------- regras

    @Test
    void tecnicoEditaUmNivelEAMudancaValeNaHora() throws Exception {
        JsonNode lagarta = null;
        for (JsonNode n : obter("/api/niveis")) {
            if (n.get("chave").asText().equals("LAGARTA")) lagarta = n;
        }
        assertNotNull(lagarta);
        long id = lagarta.get("id").asLong();
        try {
            mvc.perform(autenticado(put("/api/niveis/" + id)).content("{\"valor\":0}")).andExpect(status().isBadRequest());
            mvc.perform(autenticado(put("/api/niveis/" + id)).content("{\"valor\":15}")).andExpect(status().isOk());
        } finally {
            mvc.perform(autenticado(put("/api/niveis/" + id)).content("{\"valor\":20}")).andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------- CSV

    @Test
    void exportaCsvDoPeriodo() throws Exception {
        String hoje = LocalDate.now().toString();
        String csv = mvc.perform(get("/api/exportacao/monitoramentos.csv").param("de", LocalDate.now().minusDays(60).toString())
                        .param("ate", hoje).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String[] linhas = csv.split("\r\n");
        assertTrue(linhas[0].contains("Percevejos por pano (média)"));
        assertTrue(linhas.length > 10);
        assertEquals(14, linhas[1].split(";", -1).length);

        mvc.perform(get("/api/exportacao/monitoramentos.csv").param("de", hoje)
                        .param("ate", LocalDate.now().minusDays(1).toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void csvProtegeContraFormulasEPontoEVirgula() {
        assertEquals("'=SOMA(A1)", ExportacaoControle.celula("=SOMA(A1)"));
        assertEquals("\"a;b\"", ExportacaoControle.celula("a;b"));
        assertEquals("1,50", ExportacaoControle.celula("1,50"));
        assertEquals("-3,5", ExportacaoControle.celula("-3,5"));
    }

    // ---------------------------------------------------------------- lembretes

    @Test
    void lembreteSemanalAvisaQuemEstaHa7DiasOuMaisSemMonitorarEUmaVezSo() throws Exception {
        // Os testes compartilham o banco; o "Lote 3" (Marlene) fica parado há 8 dias e nenhum outro teste mexe nele.
        assertTrue(lembrete.enviarLembretes() >= 1);
        assertEquals(0, lembrete.enviarLembretes(), "não repete o lembrete no mesmo dia");

        JsonNode marlene = produtorPorNome("Marlene");
        boolean achou = false;
        for (JsonNode m : obter("/api/conversas/" + marlene.get("produtorId").asLong() + "/mensagens")) {
            if (m.path("texto").asText().matches("Seu Marlene, faz \\d+ dias do último pano no talhão Lote 3\\.")) achou = true;
        }
        assertTrue(achou);
    }

    @Test
    void botaoCobrarNoWhatsAppEnviaOLembrete() throws Exception {
        long id = talhaoPorNome("Sítio Boa Vista").get("id").asLong();
        mvc.perform(autenticado(post("/api/talhoes/" + id + "/cobrar"))).andExpect(status().isOk());
        JsonNode sandra = produtorPorNome("Sandra");
        assertTrue(sandra.get("ultimaMensagem").asText().contains("talhão Sítio Boa Vista"));
    }

    // ---------------------------------------------------------------- isolamento entre cooperativas

    @Test
    void tecnicoDeOutraCooperativaNaoEnxergaNemMexeNosDados() throws Exception {
        Cooperativa outra = new Cooperativa();
        outra.setNome("Outra Cooperativa");
        cooperativas.save(outra);
        Usuario intruso = new Usuario();
        intruso.setNome("Intruso");
        intruso.setEmail("intruso@outra.com");
        intruso.setSenhaHash(encoder.encode("abc123"));
        intruso.setPapel(Papel.TECNICO);
        intruso.setCooperativa(outra);
        usuarios.save(intruso);

        long talhaoDaOutra = talhaoPorNome("Sede").get("id").asLong();
        long produtorDaOutra = produtorPorNome("Valdir").get("produtorId").asLong();
        long alerta = obter("/api/alertas").get(0).get("id").asLong();
        String tokenIntruso = login("intruso@outra.com", "abc123");
        var h = "Bearer " + tokenIntruso;

        assertEquals(0, json.readTree(mvc.perform(get("/api/talhoes").header("Authorization", h)).andReturn().getResponse().getContentAsString()).size());
        assertEquals(0, json.readTree(mvc.perform(get("/api/alertas").header("Authorization", h)).andReturn().getResponse().getContentAsString()).size());
        mvc.perform(get("/api/talhoes/" + talhaoDaOutra).header("Authorization", h)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/talhoes/" + talhaoDaOutra).header("Authorization", h)).andExpect(status().isNotFound());
        mvc.perform(post("/api/talhoes/" + talhaoDaOutra + "/cobrar").header("Authorization", h)).andExpect(status().isNotFound());
        mvc.perform(get("/api/conversas/" + produtorDaOutra + "/mensagens").header("Authorization", h)).andExpect(status().isNotFound());
        mvc.perform(post("/api/alertas/" + alerta + "/confirmar").header("Authorization", h)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/produtores/" + produtorDaOutra).header("Authorization", h)).andExpect(status().isNotFound());
    }
}
