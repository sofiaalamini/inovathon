package br.com.panocerto;

import br.com.panocerto.modelo.Alerta;
import br.com.panocerto.modelo.StatusAlerta;
import br.com.panocerto.repositorio.RepositorioAlerta;
import br.com.panocerto.repositorio.RepositorioAplicacao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo pelo simulador, que passa pelo mesmo ServicoRobo do webhook real:
 * 3 pontos até a decisão, alerta para o técnico, comando "apliquei" e número desconhecido.
 * Roda sem nenhuma chave (mensageiro simulador + IA em modo demo) e com os dados de exemplo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoSimuladorIntegracaoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RepositorioAlerta alertas;
    @Autowired RepositorioAplicacao aplicacoes;

    private String token;

    @BeforeEach
    void entrar() throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"tecnico@panocerto.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = json.readTree(corpo).get("token").asText();
    }

    // ---------- ajudantes ----------

    private long produtorId(String nome) throws Exception {
        String corpo = mvc.perform(get("/api/conversas").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        for (JsonNode p : json.readTree(corpo)) {
            if (p.get("nome").asText().equals(nome)) {
                return p.get("produtorId").asLong();
            }
        }
        throw new AssertionError("Produtor não encontrado: " + nome);
    }

    private void mandar(long produtorId, String tipo, String texto, String botaoId, MockMultipartFile arquivo) throws Exception {
        var req = MockMvcRequestBuilders.multipart("/api/simulador/mensagem");
        req.param("produtorId", String.valueOf(produtorId)).param("tipo", tipo)
                .header("Authorization", "Bearer " + token);
        if (texto != null) req.param("texto", texto);
        if (botaoId != null) req.param("botaoId", botaoId);
        if (arquivo != null) req.file(arquivo);
        mvc.perform(req).andExpect(status().isOk());
    }

    private void texto(long id, String texto) throws Exception {
        mandar(id, "TEXTO", texto, null, null);
    }

    private void botao(long id, String botaoId, String titulo) throws Exception {
        mandar(id, "BOTAO", titulo, botaoId, null);
    }

    private JsonNode ultimaResposta(long produtorId) throws Exception {
        String corpo = mvc.perform(get("/api/conversas/" + produtorId + "/mensagens")
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString();
        JsonNode ultima = null;
        for (JsonNode m : json.readTree(corpo)) {
            if (m.get("direcao").asText().equals("SAIDA")) ultima = m;
        }
        assertNotNull(ultima, "o robô deveria ter respondido");
        return ultima;
    }

    private String textoDaUltimaResposta(long produtorId) throws Exception {
        return ultimaResposta(produtorId).get("texto").asText();
    }

    // ---------- testes ----------

    @Test
    void tresPontosGeramDecisaoAcimaEAlertaParaOTecnico() throws Exception {
        long joaquim = produtorId("Joaquim");   // um talhão só: não precisa escolher

        texto(joaquim, "2 percevejos 30 lagartas 10%");
        assertTrue(textoDaUltimaResposta(joaquim).contains("Faltam 2 pontos"));

        texto(joaquim, "1 percevejo 30 lagartas 10%");
        assertTrue(textoDaUltimaResposta(joaquim).contains("Falta 1 ponto"));

        long alertasAntes = alertas.count();
        texto(joaquim, "0 percevejos 30 lagartas 10%");
        String decisao = textoDaUltimaResposta(joaquim);
        assertTrue(decisao.contains("Aplique só neste talhão, com produto seletivo."), decisao);
        assertTrue(decisao.contains("técnico"), "o produtor deve saber que o técnico foi avisado");

        assertEquals(alertasAntes + 1, alertas.count());
        Alerta novo = alertas.findAll().stream().filter(a -> a.getTalhao().getNome().equals("Chácara"))
                .filter(a -> a.getStatus() == StatusAlerta.PENDENTE).findFirst().orElseThrow();
        assertNotNull(novo.getMonitoramento());

        // "resultado" repete a decisão
        texto(joaquim, "resultado");
        assertTrue(textoDaUltimaResposta(joaquim).contains("Aplique só neste talhão"));

        // Como a decisão foi ACIMA, "apliquei" é aceito e fica registrado
        long aplicacoesAntes = aplicacoes.count();
        texto(joaquim, "apliquei");
        assertTrue(textoDaUltimaResposta(joaquim).contains("Anotei a aplicação"));
        assertEquals(aplicacoesAntes + 1, aplicacoes.count());
    }

    @Test
    void doisMonitoramentosAcimaSeguidosNaoDuplicamOAlertaPendente() throws Exception {
        long ivo = produtorId("Ivo");   // Fazenda Nova: R5, grão
        for (int rodada = 0; rodada < 2; rodada++) {
            for (int ponto = 0; ponto < 3; ponto++) {
                texto(ivo, "5 percevejos 0 lagartas 0%");
            }
            assertTrue(textoDaUltimaResposta(ivo).contains("Aplique só neste talhão"));
        }
        long pendentes = alertas.findAll().stream()
                .filter(a -> a.getTalhao().getNome().equals("Fazenda Nova") && a.getStatus() == StatusAlerta.PENDENTE).count();
        assertEquals(1, pendentes);
    }

    @Test
    void apliqueiNumTalhaoQueNaoPrecisavaAvisaOProdutor() throws Exception {
        long sandra = produtorId("Sandra");   // último monitoramento: ABAIXO
        texto(sandra, "apliquei");
        assertEquals("Pelo monitoramento, este talhão não precisava. Avise o técnico.", textoDaUltimaResposta(sandra));
    }

    @Test
    void produtorComVariosTalhoesEscolheOTalhaoEFotoPassaPelaDesfolha() throws Exception {
        long valdir = produtorId("Valdir");   // 3 talhões: pergunta com botões

        texto(valdir, "2 percevejos 3 lagartas 10%");
        JsonNode pergunta = ultimaResposta(valdir);
        assertEquals("BOTOES", pergunta.get("tipo").asText());
        assertEquals(3, pergunta.get("opcoes").size());

        String idTalhao = pergunta.get("opcoes").get(0).get("id").asText();   // botão do primeiro talhão
        botao(valdir, idTalhao, "Beira do Rio");
        assertTrue(textoDaUltimaResposta(valdir).contains("escolhido"));

        // Foto: o robô conta (modo demo) e pergunta a desfolha com 3 botões
        mandar(valdir, "IMAGEM", null, null, new MockMultipartFile("arquivo", "pano.jpg", "image/jpeg", new byte[]{1, 2, 3, 4}));
        // Foto qualquer em demonstração: o robô não sabe o que tem nela, então pede para conferir
        JsonNode conferir = ultimaResposta(valdir);
        assertTrue(conferir.get("texto").asText().endsWith("Está certo?"), conferir.get("texto").asText());
        assertEquals("CONT_OK", conferir.get("opcoes").get(0).get("id").asText());
        botao(valdir, "CONT_OK", "Está certo");
        JsonNode contagem = ultimaResposta(valdir);
        assertTrue(contagem.get("texto").asText().contains("Quanto das folhas está comido?"));
        assertEquals(List.of("DESF_0", "DESF_10", "DESF_20"),
                List.of(contagem.get("opcoes").get(0).get("id").asText(),
                        contagem.get("opcoes").get(1).get("id").asText(),
                        contagem.get("opcoes").get(2).get("id").asText()));

        botao(valdir, "DESF_10", "10%");
        assertTrue(textoDaUltimaResposta(valdir).contains("registrado"));

        // A foto foi guardada na amostra, para aparecer no painel
        String conversa = mvc.perform(get("/api/conversas/" + valdir + "/mensagens")
                .header("Authorization", "Bearer " + token)).andReturn().getResponse().getContentAsString();
        boolean temFoto = false;
        for (JsonNode m : json.readTree(conversa)) {
            if (m.has("urlMidia") && m.get("urlMidia").asText().startsWith("/midias/")) temFoto = true;
        }
        assertTrue(temFoto);
    }

    @Test
    void audioEhTranscritoEOProdutorVeOQueORoboEntendeu() throws Exception {
        long marlene = produtorId("Marlene");   // 4 talhões: pergunta com lista
        texto(marlene, "oi");
        // "oi" não é contagem: o robô só explica. Depois pedimos a escolha do talhão pelo comando.
        texto(marlene, "trocar");
        JsonNode pergunta = ultimaResposta(marlene);
        assertEquals("LISTA", pergunta.get("tipo").asText());
        assertEquals(4, pergunta.get("opcoes").size());

        botao(marlene, pergunta.get("opcoes").get(0).get("id").asText(), "Lote 1");
        mandar(marlene, "AUDIO", null, null, new MockMultipartFile("arquivo", "audio.webm", "audio/webm", new byte[]{9, 8, 7}));
        String resposta = textoDaUltimaResposta(marlene);
        assertTrue(resposta.startsWith("Entendi: ") && resposta.endsWith("Está certo?"), resposta);
        assertTrue(resposta.contains("percevejo") && resposta.contains("lagarta"));
    }

    @Test
    void numeroDesconhecidoRecebeConviteParaSeCadastrar() throws Exception {
        mvc.perform(MockMvcRequestBuilders.multipart("/api/simulador/mensagem")
                        .param("desconhecido", "true").param("tipo", "TEXTO").param("texto", "oi")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String corpo = mvc.perform(get("/api/simulador/desconhecido/mensagens").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(corpo.contains("Seu número ainda não está cadastrado"), corpo);
    }

    @Test
    void apiExigeLogin() throws Exception {
        mvc.perform(get("/api/conversas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/conversas").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaErradaNaoEntra() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"tecnico@panocerto.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }
}
