package br.com.panocerto.controle;

import br.com.panocerto.controle.Dtos.TalhaoDetalhe;
import br.com.panocerto.controle.Dtos.TalhaoResumo;
import br.com.panocerto.modelo.Finalidade;
import br.com.panocerto.modelo.Talhao;
import br.com.panocerto.repositorio.RepositorioTalhao;
import br.com.panocerto.servico.Acesso;
import br.com.panocerto.servico.ServicoCadastro;
import br.com.panocerto.servico.ServicoLembrete;
import br.com.panocerto.servico.ServicoPainel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talhoes")
public class TalhaoControle {

    public record TalhaoPedido(@NotBlank(message = "Informe o nome do talhão.") String nome,
                               @NotNull(message = "Informe os hectares.")
                               @DecimalMin(value = "0.01", message = "Os hectares precisam ser maiores que zero.") BigDecimal hectares,
                               @NotBlank(message = "Informe o estádio.") String estadio,
                               @NotNull(message = "Informe a finalidade (GRAO ou SEMENTE).") Finalidade finalidade,
                               @NotNull(message = "Escolha o produtor.") Long produtorId,
                               Double latitude, Double longitude) {
    }

    private final RepositorioTalhao talhoes;
    private final ServicoCadastro cadastro;
    private final ServicoPainel painel;
    private final ServicoLembrete lembrete;
    private final Acesso acesso;

    public TalhaoControle(RepositorioTalhao talhoes, ServicoCadastro cadastro, ServicoPainel painel,
                          ServicoLembrete lembrete, Acesso acesso) {
        this.talhoes = talhoes;
        this.cadastro = cadastro;
        this.painel = painel;
        this.lembrete = lembrete;
        this.acesso = acesso;
    }

    @GetMapping
    public List<TalhaoResumo> listar() {
        return talhoes.findByProdutorCooperativaIdOrderByNome(acesso.cooperativaId()).stream().map(painel::resumo).toList();
    }

    @GetMapping("/{id}")
    public TalhaoDetalhe detalhe(@PathVariable Long id) {
        return painel.detalhe(acesso.talhao(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TalhaoResumo criar(@Valid @RequestBody TalhaoPedido p) {
        Talhao t = cadastro.criarTalhao(acesso.produtor(p.produtorId()), p.nome(), p.hectares(), p.estadio(),
                p.finalidade(), p.latitude(), p.longitude());
        return painel.resumo(t);
    }

    @PutMapping("/{id}")
    public TalhaoResumo atualizar(@PathVariable Long id, @Valid @RequestBody TalhaoPedido p) {
        Talhao t = cadastro.atualizarTalhao(acesso.talhao(id), acesso.produtor(p.produtorId()), p.nome(), p.hectares(),
                p.estadio(), p.finalidade(), p.latitude(), p.longitude());
        return painel.resumo(t);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        cadastro.excluirTalhao(acesso.talhao(id));
    }

    /** Botão "Cobrar no WhatsApp": manda o lembrete para o produtor agora. */
    @PostMapping("/{id}/cobrar")
    public Map<String, Object> cobrar(@PathVariable Long id) {
        lembrete.cobrar(acesso.talhao(id));
        return Map.of("ok", true);
    }
}
