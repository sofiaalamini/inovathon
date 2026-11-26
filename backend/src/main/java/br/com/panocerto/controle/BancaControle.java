package br.com.panocerto.controle;

import br.com.panocerto.ia.ServicoIa;
import br.com.panocerto.repositorio.RepositorioMensagemLog;
import br.com.panocerto.robo.ServicoRobo;
import br.com.panocerto.servico.ServicoBanca;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** API da página /testar, usada pela banca no próprio celular, sem login. */
@RestController
@RequestMapping("/api/banca")
public class BancaControle {

    static final String CABECALHO = "X-Visitante";

    private final ServicoBanca banca;
    private final ServicoRobo robo;
    private final ServicoIa ia;
    private final RepositorioMensagemLog historico;

    public BancaControle(ServicoBanca banca, ServicoRobo robo, ServicoIa ia, RepositorioMensagemLog historico) {
        this.banca = banca;
        this.robo = robo;
        this.ia = ia;
        this.historico = historico;
    }

    public record Entrada(String nome) {
    }

    @GetMapping
    public Map<String, Object> situacao() {
        return Map.of("ativo", banca.ativo(), "iaDemo", ia.emDemonstracao());
    }

    @PostMapping("/entrar")
    public Map<String, String> entrar(@RequestBody Entrada entrada) {
        String codigo = banca.entrar(entrada.nome());
        robo.saudar(banca.telefoneDe(codigo));
        return Map.of("codigo", codigo);
    }

    @GetMapping("/mensagens")
    public List<ConversaControle.MensagemDto> mensagens(@RequestHeader(name = CABECALHO, required = false) String codigo) {
        return historico.findByTelefoneOrderByCriadaEmAsc(banca.telefoneDe(codigo)).stream()
                .map(ConversaControle::paraDto).toList();
    }

    @GetMapping("/resumo")
    public ServicoBanca.Resumo resumo(@RequestHeader(name = CABECALHO, required = false) String codigo) {
        return banca.resumo(banca.telefoneDe(codigo));
    }

    @PostMapping(value = "/mensagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> enviar(@RequestHeader(name = CABECALHO, required = false) String codigo,
                                      @RequestParam String tipo,
                                      @RequestParam(required = false) String texto,
                                      @RequestParam(required = false) String botaoId,
                                      @RequestParam(required = false) MultipartFile arquivo) throws IOException {
        if (texto != null && texto.length() > 300) {
            texto = texto.substring(0, 300);
        }
        robo.processar(SimuladorControle.montar(banca.telefoneParaEnviar(codigo), tipo, texto, botaoId, arquivo));
        return Map.of("ok", true);
    }
}
