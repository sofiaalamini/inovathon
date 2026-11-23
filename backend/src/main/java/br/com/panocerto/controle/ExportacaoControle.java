package br.com.panocerto.controle;

import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.modelo.Decisao;
import br.com.panocerto.modelo.Monitoramento;
import br.com.panocerto.modelo.StatusMonitoramento;
import br.com.panocerto.modelo.Talhao;
import br.com.panocerto.repositorio.RepositorioAmostra;
import br.com.panocerto.repositorio.RepositorioMonitoramento;
import br.com.panocerto.servico.Acesso;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * CSV dos monitoramentos por período, para lançar no manejo.app ou numa planilha.
 * Separador ";" e números com vírgula, que é o que o Excel brasileiro espera.
 */
@RestController
@RequestMapping("/api/exportacao")
public class ExportacaoControle {

    private static final String[] COLUNAS = {"Data", "Produtor", "Talhão", "Área (ha)", "Estádio", "Finalidade",
            "Pontos amostrados", "Percevejos por pano (média)", "Lagartas por pano (média)", "Desfolha (%) (média)",
            "Decisão", "Recomendação", "Latitude", "Longitude"};

    private final RepositorioMonitoramento monitoramentos;
    private final RepositorioAmostra amostras;
    private final Acesso acesso;

    public ExportacaoControle(RepositorioMonitoramento monitoramentos, RepositorioAmostra amostras, Acesso acesso) {
        this.monitoramentos = monitoramentos;
        this.amostras = amostras;
        this.acesso = acesso;
    }

    @GetMapping("/monitoramentos.csv")
    public ResponseEntity<byte[]> exportar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        if (ate.isBefore(de)) {
            throw new ResponseStatusException(BAD_REQUEST, "A data final não pode ser antes da inicial.");
        }
        List<Monitoramento> lista = monitoramentos
                .findByTalhaoProdutorCooperativaIdAndStatusAndDataHoraBetweenOrderByDataHora(
                        acesso.cooperativaId(), StatusMonitoramento.CONCLUIDO, de.atStartOfDay(), ate.plusDays(1).atStartOfDay());

        StringBuilder csv = new StringBuilder("﻿");   // BOM: o Excel abre com acentos certos
        csv.append(String.join(";", COLUNAS)).append("\r\n");
        DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (Monitoramento m : lista) {
            Talhao t = m.getTalhao();
            Decisao d = m.getDecisao();
            String[] linha = {
                    m.getDataHora().format(formatoData),
                    t.getProdutor().getNome(),
                    t.getNome(),
                    numero(t.getHectares().doubleValue()),
                    t.getEstadio(),
                    t.getFinalidade() == null ? "" : t.getFinalidade().name(),
                    String.valueOf(amostras.countByMonitoramentoId(m.getId())),
                    numero(m.getMediaPercevejos()),
                    numero(m.getMediaLagartas()),
                    numero(m.getMediaDesfolha()),
                    d == null ? "" : d.name(),
                    d == null ? "" : RegraMip.mensagem(d),
                    t.getLatitude() == null ? "" : String.valueOf(t.getLatitude()),
                    t.getLongitude() == null ? "" : String.valueOf(t.getLongitude())};
            csv.append(String.join(";", java.util.Arrays.stream(linha).map(ExportacaoControle::celula).toList())).append("\r\n");
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"panocerto-monitoramentos-" + de + "-a-" + ate + ".csv\"")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String numero(Double valor) {
        return valor == null ? "" : String.format(Locale.of("pt", "BR"), "%.2f", valor);
    }

    /** Protege o CSV: aspas quando preciso e apóstrofo antes de fórmulas (=, +, -, @) digitadas por usuários. */
    public static String celula(String valor) {
        String v = valor == null ? "" : valor;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?[\\d.,]+")) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
