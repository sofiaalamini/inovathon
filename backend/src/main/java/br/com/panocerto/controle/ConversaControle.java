package br.com.panocerto.controle;

import br.com.panocerto.modelo.MensagemLog;
import br.com.panocerto.modelo.Produtor;
import br.com.panocerto.repositorio.RepositorioMensagemLog;
import br.com.panocerto.repositorio.RepositorioProdutor;
import br.com.panocerto.servico.Acesso;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** Histórico de mensagens de cada produtor (tela "Conversas" e tela do simulador). */
@RestController
@RequestMapping("/api/conversas")
public class ConversaControle {

    public record OpcaoDto(String id, String titulo) {
    }

    public record MensagemDto(Long id, String direcao, String tipo, String texto, List<OpcaoDto> opcoes,
                              String urlMidia, LocalDateTime criadaEm) {
    }

    public record ResumoDto(Long produtorId, String nome, String telefone, String ultimaMensagem, LocalDateTime ultimaEm) {
    }

    private final RepositorioProdutor produtores;
    private final RepositorioMensagemLog historico;
    private final Acesso acesso;

    public ConversaControle(RepositorioProdutor produtores, RepositorioMensagemLog historico, Acesso acesso) {
        this.produtores = produtores;
        this.historico = historico;
        this.acesso = acesso;
    }

    @GetMapping
    public List<ResumoDto> listar() {
        return produtores.findByCooperativaIdOrderByNome(acesso.cooperativaId()).stream().map(p -> {
            var ultima = historico.findFirstByTelefoneOrderByCriadaEmDesc(p.getTelefone());
            return new ResumoDto(p.getId(), p.getNome(), p.getTelefone(),
                    ultima.map(MensagemLog::getTexto).orElse(null),
                    ultima.map(MensagemLog::getCriadaEm).orElse(null));
        }).toList();
    }

    @GetMapping("/{produtorId}/mensagens")
    public List<MensagemDto> mensagens(@PathVariable Long produtorId) {
        Produtor produtor = acesso.produtor(produtorId);
        return historico.findByTelefoneOrderByCriadaEmAsc(produtor.getTelefone()).stream().map(ConversaControle::paraDto).toList();
    }

    public static MensagemDto paraDto(MensagemLog m) {
        List<OpcaoDto> opcoes = m.getOpcoes() == null || m.getOpcoes().isBlank() ? List.of()
                : Arrays.stream(m.getOpcoes().split("\n")).map(linha -> {
                    String[] partes = linha.split("\\|", 2);
                    return new OpcaoDto(partes[0], partes.length > 1 ? partes[1] : partes[0]);
                }).toList();
        return new MensagemDto(m.getId(), m.getDirecao().name(), m.getTipo().name(), m.getTexto(), opcoes,
                m.getUrlMidia(), m.getCriadaEm());
    }
}
