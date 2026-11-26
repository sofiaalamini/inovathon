package br.com.panocerto;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** A página da banca: entra sem login, conversa com o robô e só vê a própria conversa. */
@SpringBootTest(properties = {"panocerto.modo-banca=true",
        "spring.datasource.url=jdbc:h2:mem:banca;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BancaApiTest {

    @Autowired
    private MockMvc api;

    private String entrar(String nome) throws Exception {
        String corpo = api.perform(post("/api/banca/entrar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.codigo");
    }

    @Test
    void visitanteConversaEVeSoAPropriaConversa() throws Exception {
        String ana = entrar("Ana");
        String bruno = entrar("Bruno");

        api.perform(multipart("/api/banca/mensagem").header("X-Visitante", ana)
                        .param("tipo", "TEXTO").param("texto", "ajuda"))
                .andExpect(status().isOk());

        api.perform(get("/api/banca/mensagens").header("X-Visitante", ana))
                .andExpect(jsonPath("$.length()").value(3));
        api.perform(get("/api/banca/mensagens").header("X-Visitante", bruno))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void resumoMostraProgressoEResultado() throws Exception {
        String carla = entrar("Carla");
        mandar(carla, "BOTAO", "Sede", primeiroBotao(carla));
        mandar(carla, "TEXTO", "3 percevejos 4 lagartas 10%", null);
        api.perform(get("/api/banca/resumo").header("X-Visitante", carla))
                .andExpect(jsonPath("$.talhao").value("Sede"))
                .andExpect(jsonPath("$.pontos.length()").value(1))
                .andExpect(jsonPath("$.resultado").doesNotExist());

        mandar(carla, "TEXTO", "3 percevejos 4 lagartas 10%", null);
        mandar(carla, "TEXTO", "3 percevejos 4 lagartas 10%", null);
        api.perform(get("/api/banca/resumo").header("X-Visitante", carla))
                .andExpect(jsonPath("$.resultado.decisao").value("ACIMA"))
                .andExpect(jsonPath("$.resultado.alerta").value("PENDENTE"))
                // aplica só na Sede: os 12 ha da Várzea ficam sem pulverizar (12 x 120)
                .andExpect(jsonPath("$.resultado.economia").value(1440.0));
    }

    private String primeiroBotao(String codigo) throws Exception {
        String corpo = api.perform(get("/api/banca/mensagens").header("X-Visitante", codigo))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$[0].opcoes[0].id");
    }

    private void mandar(String codigo, String tipo, String texto, String botaoId) throws Exception {
        var pedido = multipart("/api/banca/mensagem").header("X-Visitante", codigo)
                .param("tipo", tipo).param("texto", texto);
        if (botaoId != null) {
            pedido.param("botaoId", botaoId);
        }
        api.perform(pedido).andExpect(status().isOk());
    }

    @Test
    void numeroAltoPedeConfirmacaoEFotoQueNaoEhPanoNaoRegistra() throws Exception {
        String dani = entrar("Dani");
        mandar(dani, "BOTAO", "Sede", primeiroBotao(dani));
        mandar(dani, "TEXTO", "80 percevejos 2 lagartas 10%", null);
        String corpo = api.perform(get("/api/banca/mensagens").header("X-Visitante", dani))
                .andReturn().getResponse().getContentAsString();
        String ultima = JsonPath.read(corpo, "$[-1].texto");
        org.junit.jupiter.api.Assertions.assertTrue(ultima.contains("está certo?"), ultima);
        api.perform(get("/api/banca/resumo").header("X-Visitante", dani)).andExpect(jsonPath("$.pontos.length()").value(0));

        // confirmou: a desfolha que veio junto é usada, sem perguntar de novo
        mandar(dani, "BOTAO", "Sim", "CONT_OK");
        api.perform(get("/api/banca/resumo").header("X-Visitante", dani)).andExpect(jsonPath("$.pontos[0].desfolha").value(10));
    }

    @Test
    void visitanteNaoEntraNosIndicadores() throws Exception {
        String token = JsonPath.read(api.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tecnico@panocerto.com\",\"senha\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString(), "$.token");
        String antes = api.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        entrar("Eva");
        String depois = api.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals((Double) JsonPath.read(antes, "$.economiaEstimada"),
                (Double) JsonPath.read(depois, "$.economiaEstimada"));
    }

    @Test
    void codigoInventadoENomeVazioSaoRecusados() throws Exception {
        api.perform(get("/api/banca/mensagens").header("X-Visitante", "inventado"))
                .andExpect(status().isGone());
        api.perform(post("/api/banca/entrar").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visitanteNaoAcessaOPainel() throws Exception {
        api.perform(get("/api/produtores")).andExpect(status().isUnauthorized());
    }
}
