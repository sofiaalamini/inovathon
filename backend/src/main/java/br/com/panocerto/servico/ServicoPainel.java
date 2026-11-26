package br.com.panocerto.servico;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.config.Relogio;
import br.com.panocerto.controle.Dtos.*;
import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/** Monta o que o painel mostra: dashboard, resumo e histórico dos talhões, alertas. */
@Service
public class ServicoPainel {

    /** Calendário fixo de comparação: aplicações por safra sem usar o MIP. */
    public static final double APLICACOES_DO_CALENDARIO = 3.6;
    public static final int DIAS_SEM_MONITORAR = 7;
    private static final int SEMANAS_NO_GRAFICO = 12;

    private final Configuracao config;
    private final RepositorioTalhao talhoes;
    private final RepositorioMonitoramento monitoramentos;
    private final RepositorioAmostra amostras;
    private final RepositorioAplicacao aplicacoes;
    private final RepositorioAlerta alertas;

    public ServicoPainel(Configuracao config, RepositorioTalhao talhoes, RepositorioMonitoramento monitoramentos,
                         RepositorioAmostra amostras, RepositorioAplicacao aplicacoes, RepositorioAlerta alertas) {
        this.config = config;
        this.talhoes = talhoes;
        this.monitoramentos = monitoramentos;
        this.amostras = amostras;
        this.aplicacoes = aplicacoes;
        this.alertas = alertas;
    }

    // ---------------------------------------------------------------- talhão

    public TalhaoResumo resumo(Talhao t) {
        Optional<Monitoramento> ultimoFechado = monitoramentos
                .findFirstByTalhaoIdAndStatusOrderByDataHoraDesc(t.getId(), StatusMonitoramento.CONCLUIDO);
        Optional<Monitoramento> ultimoQualquer = monitoramentos
                .findFirstByTalhaoIdAndStatusNotOrderByDataHoraDesc(t.getId(), StatusMonitoramento.CANCELADO);

        LocalDateTime ultimo = ultimoQualquer.map(Monitoramento::getDataHora).orElse(null);
        // Talhão que nunca foi monitorado conta os dias desde o cadastro.
        LocalDateTime referencia = ultimo != null ? ultimo : t.getCriadoEm();
        long dias = referencia == null ? 0 : ChronoUnit.DAYS.between(referencia.toLocalDate(), Relogio.agora().toLocalDate());

        Decisao decisao = ultimoFechado.map(Monitoramento::getDecisao).orElse(null);
        Long alertaId = null;
        boolean aplicado = false;
        if (decisao == Decisao.ACIMA) {
            // O alerta do último monitoramento; se já foi confirmado, o talhão fica "Aplicado".
            Optional<Alerta> alerta = alertas.findFirstByTalhaoIdOrderByCriadoEmDesc(t.getId());
            if (alerta.isPresent() && alerta.get().getStatus() == StatusAlerta.CONFIRMADO) {
                aplicado = true;
            } else {
                alertaId = alerta.map(Alerta::getId).orElse(null);
            }
        }
        return new TalhaoResumo(t.getId(), t.getNome(), t.getProdutor().getId(), t.getProdutor().getNome(),
                t.getHectares(), t.getEstadio(), t.getFinalidade(), t.getLatitude(), t.getLongitude(),
                decisao, ultimo, dias,
                ultimoFechado.map(Monitoramento::getMediaPercevejos).orElse(null),
                ultimoFechado.map(Monitoramento::getMediaLagartas).orElse(null),
                ultimoFechado.map(Monitoramento::getMediaDesfolha).orElse(null),
                alertaId, aplicado, t.getProdutor().isVisitante());
    }

    public TalhaoDetalhe detalhe(Talhao t) {
        List<MonitoramentoDto> historico = monitoramentos.findByTalhaoIdOrderByDataHoraDesc(t.getId()).stream()
                .filter(m -> m.getStatus() != StatusMonitoramento.CANCELADO)
                .map(m -> new MonitoramentoDto(m.getId(), m.getDataHora(), m.getStatus(), m.getDecisao(),
                        m.getDecisao() == null ? null : RegraMip.mensagem(m.getDecisao()),
                        m.getMediaPercevejos(), m.getMediaLagartas(), m.getMediaDesfolha(),
                        amostras.findByMonitoramentoIdOrderByCriadaEm(m.getId()).stream()
                                .map(a -> new AmostraDto(a.getId(), a.getPercevejos(), a.getLagartas(), a.getDesfolha(),
                                        a.getOrigem(), a.getUrlMidia(), a.getTranscricao(), a.getCriadaEm()))
                                .toList()))
                .toList();
        List<AplicacaoDto> apps = aplicacoes.findByTalhaoIdOrderByDataHoraDesc(t.getId()).stream()
                .map(a -> new AplicacaoDto(a.getId(), a.getDataHora(), a.isConfirmada())).toList();
        return new TalhaoDetalhe(resumo(t), historico, apps);
    }

    // ---------------------------------------------------------------- alertas

    public AlertaDto alertaDto(Alerta a) {
        Talhao t = a.getTalhao();
        Monitoramento m = a.getMonitoramento();
        return new AlertaDto(a.getId(), a.getStatus(), a.getCriadoEm(), a.getResolvidoEm(), t.getId(), t.getNome(),
                t.getProdutor().getNome(), t.getProdutor().getTelefone(), t.getHectares(), t.getEstadio(),
                m == null ? null : m.getDecisao(),
                m == null ? null : m.getMediaPercevejos(), m == null ? null : m.getMediaLagartas(),
                m == null ? null : m.getMediaDesfolha());
    }

    // ---------------------------------------------------------------- dashboard

    public Dashboard dashboard(Long cooperativaId) {
        List<Talhao> todos = talhoes.findByProdutorCooperativaIdOrderByNome(cooperativaId);
        List<TalhaoResumo> resumos = todos.stream().map(this::resumo).toList();

        // Cada talhão aparece num grupo só: aplicar > sem monitorar (o "perto" antigo já venceu) > perto.
        List<TalhaoResumo> acima = resumos.stream().filter(r -> r.decisao() == Decisao.ACIMA && !r.aplicado()).toList();
        List<TalhaoResumo> perto = resumos.stream()
                .filter(r -> r.decisao() == Decisao.PERTO && r.diasSemMonitorar() < DIAS_SEM_MONITORAR).toList();
        List<TalhaoResumo> semMonitoramento = resumos.stream()
                .filter(r -> r.diasSemMonitorar() >= DIAS_SEM_MONITORAR && !acima.contains(r))
                .sorted(Comparator.comparingLong(TalhaoResumo::diasSemMonitorar).reversed()).toList();

        // Aplicações evitadas: o calendário fixo (3,6 por safra) menos o que foi aplicado de fato.
        LocalDateTime inicio = inicioDaSafra();
        // Visitantes da banca ficam fora dos indicadores gerais.
        Map<Long, Long> feitasPorTalhao = aplicacoes
                .findByTalhaoProdutorCooperativaIdAndDataHoraAfter(cooperativaId, inicio).stream()
                .collect(Collectors.groupingBy(a -> a.getTalhao().getId(), Collectors.counting()));
        double evitadas = 0;
        double economia = 0;
        int feitas = 0;
        List<Talhao> daCooperativa = todos.stream().filter(t -> !t.getProdutor().isVisitante()).toList();
        for (Talhao t : daCooperativa) {
            long f = feitasPorTalhao.getOrDefault(t.getId(), 0L);
            feitas += (int) f;
            double evitadasDoTalhao = Math.max(0, APLICACOES_DO_CALENDARIO - f);
            evitadas += evitadasDoTalhao;
            economia += evitadasDoTalhao * t.getHectares().doubleValue() * config.custoAplicacaoHa();
        }

        return new Dashboard(acima, perto, semMonitoramento,
                arredondar(evitadas), arredondar(APLICACOES_DO_CALENDARIO * daCooperativa.size()), feitas,
                arredondar(economia), config.custoAplicacaoHa(), pressaoSemanal(cooperativaId));
    }

    /** Total de percevejos contados por semana, somando todos os talhões da cooperativa. */
    List<PressaoSemanal> pressaoSemanal(Long cooperativaId) {
        LocalDate segundaDestaSemana = Relogio.agora().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeiraSemana = segundaDestaSemana.minusWeeks(SEMANAS_NO_GRAFICO - 1L);

        Map<LocalDate, int[]> porSemana = new TreeMap<>();   // [total de percevejos, número de amostras]
        for (int i = 0; i < SEMANAS_NO_GRAFICO; i++) {
            porSemana.put(primeiraSemana.plusWeeks(i), new int[]{0, 0});
        }
        for (Amostra a : amostras.findByMonitoramentoTalhaoProdutorCooperativaIdAndCriadaEmAfter(
                cooperativaId, primeiraSemana.atStartOfDay())) {
            if (a.getMonitoramento().getTalhao().getProdutor().isVisitante()) {
                continue;   // o que a banca testa não entra no gráfico da cooperativa
            }
            LocalDate semana = a.getCriadaEm().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            int[] valores = porSemana.get(semana);
            if (valores != null) {
                valores[0] += a.getPercevejos();
                valores[1] += 1;
            }
        }
        DateTimeFormatter rotulo = DateTimeFormatter.ofPattern("dd/MM");
        return porSemana.entrySet().stream().map(e -> new PressaoSemanal(
                e.getKey().toString(), e.getKey().format(rotulo), e.getValue()[0], e.getValue()[1],
                e.getValue()[1] == 0 ? 0 : arredondar((double) e.getValue()[0] / e.getValue()[1]))).toList();
    }

    /** A safra de soja começa em 1º de setembro. */
    LocalDateTime inicioDaSafra() {
        LocalDate hoje = Relogio.agora().toLocalDate();
        int ano = hoje.getMonthValue() >= 9 ? hoje.getYear() : hoje.getYear() - 1;
        return LocalDate.of(ano, 9, 1).atStartOfDay();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
