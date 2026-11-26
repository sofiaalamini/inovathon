package br.com.panocerto.controle;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.controle.Dtos.AlertaDto;
import br.com.panocerto.controle.Dtos.Dashboard;
import br.com.panocerto.ia.ServicoIa;
import br.com.panocerto.modelo.NivelAcao;
import br.com.panocerto.repositorio.RepositorioAlerta;
import br.com.panocerto.servico.Acesso;
import br.com.panocerto.servico.ServicoAlertas;
import br.com.panocerto.servico.ServicoNiveis;
import br.com.panocerto.servico.ServicoPainel;
import br.com.panocerto.whatsapp.MensageiroWhatsApp;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Telas Início, Alertas, Regras e o aviso de modo (simulador ou WhatsApp real). */
@RestController
@RequestMapping("/api")
public class PainelControle {

    public record NivelPedido(@NotNull(message = "Informe o valor.") Double valor) {
    }

    private final ServicoPainel painel;
    private final ServicoAlertas servicoAlertas;
    private final ServicoNiveis niveis;
    private final RepositorioAlerta alertas;
    private final MensageiroWhatsApp mensageiro;
    private final ServicoIa ia;
    private final Configuracao config;
    private final Acesso acesso;

    public PainelControle(ServicoPainel painel, ServicoAlertas servicoAlertas, ServicoNiveis niveis,
                          RepositorioAlerta alertas, MensageiroWhatsApp mensageiro, ServicoIa ia,
                          Configuracao config, Acesso acesso) {
        this.painel = painel;
        this.servicoAlertas = servicoAlertas;
        this.niveis = niveis;
        this.alertas = alertas;
        this.mensageiro = mensageiro;
        this.ia = ia;
        this.config = config;
        this.acesso = acesso;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard() {
        return painel.dashboard(acesso.cooperativaId());
    }

    // ---------------------------------------------------------------- alertas

    @GetMapping("/alertas")
    public List<AlertaDto> alertas() {
        return alertas.findByTalhaoProdutorCooperativaIdOrderByCriadoEmDesc(acesso.cooperativaId())
                .stream().map(painel::alertaDto).toList();
    }

    @PostMapping("/alertas/{id}/confirmar")
    public AlertaDto confirmar(@PathVariable Long id) {
        return painel.alertaDto(servicoAlertas.confirmar(acesso.alerta(id)));
    }

    @PostMapping("/alertas/{id}/revisitar")
    public AlertaDto revisitar(@PathVariable Long id) {
        return painel.alertaDto(servicoAlertas.revisitar(acesso.alerta(id)));
    }

    // ---------------------------------------------------------------- regras

    @GetMapping("/niveis")
    public List<NivelAcao> niveis() {
        return niveis.listar();
    }

    @PutMapping("/niveis/{id}")
    public NivelAcao atualizarNivel(@PathVariable Long id, @Valid @RequestBody NivelPedido pedido) {
        return niveis.atualizar(id, pedido.valor());
    }

    // ---------------------------------------------------------------- sistema

    /** Mostra no painel se está no simulador ou no WhatsApp real, e quais IAs estão ligadas. */
    @GetMapping("/sistema")
    public Map<String, Object> sistema() {
        return Map.of(
                "whatsapp", mensageiro.modo(),
                "iaDemo", ia.emDemonstracao(),
                "planoB", ia.planoB(),
                "custoAplicacaoHa", config.custoAplicacaoHa());
    }

    public record PlanoB(boolean ativo) {
    }

    /** Plano B da apresentação: a IA para de ser chamada e responde com a demonstração. */
    @org.springframework.web.bind.annotation.PostMapping("/sistema/plano-b")
    public Map<String, Object> planoB(@org.springframework.web.bind.annotation.RequestBody PlanoB pedido) {
        ia.definirPlanoB(pedido.ativo());
        return Map.of("planoB", ia.planoB());
    }
}
