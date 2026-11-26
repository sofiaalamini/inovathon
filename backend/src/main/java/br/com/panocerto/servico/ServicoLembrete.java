package br.com.panocerto.servico;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.config.Relogio;
import br.com.panocerto.controle.Dtos.TalhaoResumo;
import br.com.panocerto.modelo.Talhao;
import br.com.panocerto.repositorio.RepositorioTalhao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

/** Lembrete semanal: avisa o produtor que está há 7 dias ou mais sem bater o pano. */
@Service
public class ServicoLembrete {

    private static final Logger log = LoggerFactory.getLogger(ServicoLembrete.class);

    private final Configuracao config;
    private final RepositorioTalhao talhoes;
    private final ServicoPainel painel;
    private final ServicoEnvio envio;

    public ServicoLembrete(Configuracao config, RepositorioTalhao talhoes, ServicoPainel painel, ServicoEnvio envio) {
        this.config = config;
        this.talhoes = talhoes;
        this.painel = painel;
        this.envio = envio;
    }

    /** Todo dia às 7h (horário de Brasília). */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    public void lembretesDiarios() {
        int enviados = enviarLembretes();
        log.info("Lembretes diários: {} enviado(s).", enviados);
    }

    /**
     * Manda o lembrete de cada talhão com {@value ServicoPainel#DIAS_SEM_MONITORAR}+ dias sem monitorar.
     * O mesmo talhão só é lembrado de novo depois de 7 dias, para não incomodar todo dia.
     */
    public int enviarLembretes() {
        int enviados = 0;
        for (Talhao talhao : talhoes.findAll()) {
            TalhaoResumo resumo = painel.resumo(talhao);
            if (resumo.diasSemMonitorar() < ServicoPainel.DIAS_SEM_MONITORAR) {
                continue;
            }
            boolean lembradoHaPouco = talhao.getUltimoLembreteEm() != null
                    && ChronoUnit.DAYS.between(talhao.getUltimoLembreteEm(), Relogio.agora()) < ServicoPainel.DIAS_SEM_MONITORAR;
            if (lembradoHaPouco) {
                continue;
            }
            try {
                lembrar(talhao, resumo.diasSemMonitorar());
                enviados++;
            } catch (Exception e) {
                log.error("Falha ao lembrar o talhão {}: {}", talhao.getId(), e.getMessage());
            }
        }
        return enviados;
    }

    /** Botão "Cobrar no WhatsApp" do painel: manda o lembrete agora, para um talhão. */
    public void cobrar(Talhao talhao) {
        lembrar(talhao, painel.resumo(talhao).diasSemMonitorar());
    }

    private void lembrar(Talhao talhao, long dias) {
        String nome = talhao.getProdutor().getNome();
        String texto = "Olá, " + nome + "! Faz " + dias + " dias do último pano no talhão " + talhao.getNome() + ".";
        // Fora da janela de 24h o envio usa o template aprovado pela Meta (variáveis {{1}}, {{2}}, {{3}}).
        envio.lembrete(talhao.getProdutor(), config.whatsappTemplateLembrete(),
                List.of(nome, String.valueOf(dias), talhao.getNome()), texto);
        talhao.setUltimoLembreteEm(Relogio.agora());
        talhoes.save(talhao);
    }
}
