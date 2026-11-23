package br.com.panocerto.controle;

import br.com.panocerto.modelo.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Formatos de resposta da API (o que o painel recebe). Entidades nunca saem direto para o JSON. */
public final class Dtos {

    private Dtos() {
    }

    public record TalhaoResumo(Long id, String nome, Long produtorId, String produtorNome, BigDecimal hectares,
                               String estadio, Finalidade finalidade, Double latitude, Double longitude,
                               Decisao decisao, LocalDateTime ultimoMonitoramentoEm, long diasSemMonitorar,
                               Double mediaPercevejos, Double mediaLagartas, Double mediaDesfolha, Long alertaId) {
    }

    public record AmostraDto(Long id, int percevejos, int lagartas, int desfolha, Origem origem,
                             String urlMidia, String transcricao, LocalDateTime criadaEm) {
    }

    public record MonitoramentoDto(Long id, LocalDateTime dataHora, StatusMonitoramento status, Decisao decisao,
                                   String recomendacao, Double mediaPercevejos, Double mediaLagartas,
                                   Double mediaDesfolha, List<AmostraDto> amostras) {
    }

    public record AplicacaoDto(Long id, LocalDateTime dataHora, boolean confirmada) {
    }

    public record TalhaoDetalhe(TalhaoResumo talhao, List<MonitoramentoDto> monitoramentos, List<AplicacaoDto> aplicacoes) {
    }

    public record AlertaDto(Long id, StatusAlerta status, LocalDateTime criadoEm, LocalDateTime resolvidoEm,
                            Long talhaoId, String talhaoNome, String produtorNome, String produtorTelefone,
                            BigDecimal hectares, String estadio, Decisao decisao,
                            Double mediaPercevejos, Double mediaLagartas, Double mediaDesfolha) {
    }

    public record PressaoSemanal(String semana, String rotulo, int totalPercevejos, int amostras, double mediaPorPano) {
    }

    public record Dashboard(List<TalhaoResumo> acima, List<TalhaoResumo> perto, List<TalhaoResumo> semMonitoramento,
                            double aplicacoesEvitadas, double aplicacoesCalendario, int aplicacoesFeitas,
                            double economiaEstimada, double custoAplicacaoHa, List<PressaoSemanal> pressaoSemanal) {
    }

    public record ProdutorDto(Long id, String nome, String telefone, List<TalhaoResumo> talhoes) {
    }
}
