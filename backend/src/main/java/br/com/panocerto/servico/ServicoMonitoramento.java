package br.com.panocerto.servico;

import br.com.panocerto.config.Relogio;
import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.mip.RegraMip.Ponto;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Registra pontos de pano de batida, fecha o monitoramento com 3 pontos e cria o alerta se for ACIMA. */
@Service
public class ServicoMonitoramento {

    /** Uma aplicação só "precisava" se a decisão ACIMA for recente. */
    private static final int DIAS_VALIDADE_DA_DECISAO = 14;

    /** Resultado de registrar um ponto. "resultado" e "alerta" só existem quando o monitoramento fechou. */
    public record ResultadoAmostra(Monitoramento monitoramento, int pontos, RegraMip.Resultado resultado, Alerta alerta) {
    }

    private final RepositorioMonitoramento monitoramentos;
    private final RepositorioAmostra amostras;
    private final RepositorioAlerta alertas;
    private final RepositorioAplicacao aplicacoes;
    private final ServicoNiveis niveis;

    public ServicoMonitoramento(RepositorioMonitoramento monitoramentos, RepositorioAmostra amostras,
                                RepositorioAlerta alertas, RepositorioAplicacao aplicacoes, ServicoNiveis niveis) {
        this.monitoramentos = monitoramentos;
        this.amostras = amostras;
        this.alertas = alertas;
        this.aplicacoes = aplicacoes;
        this.niveis = niveis;
    }

    @Transactional
    public ResultadoAmostra registrarAmostra(Talhao talhao, int percevejos, int lagartas, int desfolha,
                                             Origem origem, String urlMidia, String transcricao) {
        Monitoramento monitoramento = monitoramentoAberto(talhao).orElseGet(() -> abrir(talhao));

        Amostra amostra = new Amostra();
        amostra.setMonitoramento(monitoramento);
        amostra.setPercevejos(percevejos);
        amostra.setLagartas(lagartas);
        amostra.setDesfolha(desfolha);
        amostra.setOrigem(origem);
        amostra.setUrlMidia(urlMidia);
        amostra.setTranscricao(transcricao);
        amostra.setCriadaEm(Relogio.agora());
        amostras.save(amostra);

        List<Amostra> todas = amostras.findByMonitoramentoIdOrderByCriadaEm(monitoramento.getId());
        if (todas.size() < RegraMip.MINIMO_PONTOS) {
            return new ResultadoAmostra(monitoramento, todas.size(), null, null);
        }

        // Pontos suficientes: a RegraMip decide (nunca a IA).
        List<Ponto> pontos = todas.stream()
                .map(a -> new Ponto(a.getPercevejos(), a.getLagartas(), a.getDesfolha())).toList();
        RegraMip.Resultado resultado = niveis.regra().decidir(pontos, talhao.getEstadio(), talhao.getFinalidade());

        monitoramento.setStatus(StatusMonitoramento.CONCLUIDO);
        monitoramento.setDecisao(resultado.decisao());
        monitoramento.setMediaPercevejos(resultado.mediaPercevejos());
        monitoramento.setMediaLagartas(resultado.mediaLagartas());
        monitoramento.setMediaDesfolha(resultado.mediaDesfolha());
        monitoramentos.save(monitoramento);

        Alerta alerta = null;
        if (resultado.decisao() == Decisao.ACIMA) {
            // Já existe alerta esperando o técnico? Atualiza para o monitoramento mais novo, sem duplicar.
            alerta = alertas.findFirstByTalhaoIdAndStatusOrderByCriadoEmDesc(talhao.getId(), StatusAlerta.PENDENTE)
                    .orElseGet(Alerta::new);
            alerta.setTalhao(talhao);
            alerta.setMonitoramento(monitoramento);
            alerta.setCriadoEm(Relogio.agora());
            alerta.setStatus(StatusAlerta.PENDENTE);
            alertas.save(alerta);
        }
        return new ResultadoAmostra(monitoramento, todas.size(), resultado, alerta);
    }

    public Optional<Monitoramento> monitoramentoAberto(Talhao talhao) {
        return monitoramentos.findFirstByTalhaoIdAndStatusOrderByDataHoraDesc(talhao.getId(), StatusMonitoramento.ABERTO);
    }

    public Optional<Monitoramento> ultimoConcluido(Talhao talhao) {
        return monitoramentos.findFirstByTalhaoIdAndStatusOrderByDataHoraDesc(talhao.getId(), StatusMonitoramento.CONCLUIDO);
    }

    public long pontosDoMonitoramento(Monitoramento monitoramento) {
        return amostras.countByMonitoramentoId(monitoramento.getId());
    }

    /** Comando "novo": descarta o monitoramento que ficou pela metade. */
    public void cancelarAberto(Talhao talhao) {
        monitoramentoAberto(talhao).ifPresent(m -> {
            m.setStatus(StatusMonitoramento.CANCELADO);
            monitoramentos.save(m);
        });
    }

    /** O último monitoramento fechado deu ACIMA, há pouco tempo? Então aplicar fazia sentido. */
    public boolean aplicacaoFazSentido(Talhao talhao) {
        LocalDateTime limite = Relogio.agora().minusDays(DIAS_VALIDADE_DA_DECISAO);
        return ultimoConcluido(talhao)
                .filter(m -> m.getDecisao() == Decisao.ACIMA && m.getDataHora().isAfter(limite))
                .isPresent();
    }

    /** O produtor avisou "apliquei": guarda como aplicação ainda não confirmada pelo técnico. */
    public Aplicacao registrarAplicacaoDoProdutor(Talhao talhao) {
        Aplicacao aplicacao = new Aplicacao();
        aplicacao.setTalhao(talhao);
        aplicacao.setDataHora(Relogio.agora());
        aplicacao.setConfirmada(false);
        return aplicacoes.save(aplicacao);
    }

    private Monitoramento abrir(Talhao talhao) {
        Monitoramento m = new Monitoramento();
        m.setTalhao(talhao);
        m.setDataHora(Relogio.agora());
        m.setStatus(StatusMonitoramento.ABERTO);
        return monitoramentos.save(m);
    }
}
