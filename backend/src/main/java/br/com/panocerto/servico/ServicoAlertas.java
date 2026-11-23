package br.com.panocerto.servico;

import br.com.panocerto.config.Relogio;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.RepositorioAlerta;
import br.com.panocerto.repositorio.RepositorioAplicacao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Resposta do técnico a um alerta: confirmar a aplicação ou pedir para revisitar. O produtor é avisado no WhatsApp. */
@Service
public class ServicoAlertas {

    private final RepositorioAlerta alertas;
    private final RepositorioAplicacao aplicacoes;
    private final ServicoEnvio envio;

    public ServicoAlertas(RepositorioAlerta alertas, RepositorioAplicacao aplicacoes, ServicoEnvio envio) {
        this.alertas = alertas;
        this.aplicacoes = aplicacoes;
        this.envio = envio;
    }

    @Transactional
    public Alerta confirmar(Alerta alerta) {
        if (alerta.getStatus() == StatusAlerta.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alerta já foi confirmado.");
        }
        alerta.setStatus(StatusAlerta.CONFIRMADO);
        alerta.setResolvidoEm(Relogio.agora());
        alertas.save(alerta);

        // Se o produtor já avisou "apliquei" depois do alerta, só confirma; senão registra a aplicação.
        Talhao talhao = alerta.getTalhao();
        Aplicacao aplicacao = aplicacoes
                .findFirstByTalhaoIdAndConfirmadaFalseAndDataHoraAfterOrderByDataHoraDesc(talhao.getId(), alerta.getCriadoEm().minusMinutes(1))
                .orElseGet(() -> {
                    Aplicacao nova = new Aplicacao();
                    nova.setTalhao(talhao);
                    nova.setDataHora(Relogio.agora());
                    return nova;
                });
        aplicacao.setConfirmada(true);
        aplicacoes.save(aplicacao);

        envio.texto(talhao.getProdutor(), "O técnico confirmou: aplique só no talhão " + talhao.getNome()
                + ", com produto seletivo.");
        return alerta;
    }

    @Transactional
    public Alerta revisitar(Alerta alerta) {
        if (alerta.getStatus() != StatusAlerta.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alerta já foi respondido.");
        }
        alerta.setStatus(StatusAlerta.REVISITAR);
        alerta.setResolvidoEm(Relogio.agora());
        alertas.save(alerta);

        Talhao talhao = alerta.getTalhao();
        envio.texto(talhao.getProdutor(), "O técnico pediu para bater o pano de novo no talhão " + talhao.getNome()
                + " em 3 dias, antes de aplicar.");
        return alerta;
    }
}
