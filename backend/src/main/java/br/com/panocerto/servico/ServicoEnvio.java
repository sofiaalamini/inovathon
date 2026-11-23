package br.com.panocerto.servico;

import br.com.panocerto.config.Relogio;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.RepositorioMensagemLog;
import br.com.panocerto.whatsapp.MensageiroWhatsApp;
import br.com.panocerto.whatsapp.MensageiroWhatsApp.Opcao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Único caminho para falar com o produtor: grava a mensagem no histórico e manda pelo mensageiro
 * (real ou simulador). Se o envio falhar, só registra o erro: nunca derruba o fluxo.
 */
@Service
public class ServicoEnvio {

    private static final Logger log = LoggerFactory.getLogger(ServicoEnvio.class);

    private final MensageiroWhatsApp mensageiro;
    private final RepositorioMensagemLog historico;

    public ServicoEnvio(MensageiroWhatsApp mensageiro, RepositorioMensagemLog historico) {
        this.mensageiro = mensageiro;
        this.historico = historico;
    }

    public void texto(Produtor produtor, String texto) {
        registrar(produtor, produtor.getTelefone(), Direcao.SAIDA, TipoMensagem.TEXTO, texto, null, null, null);
        tentar(() -> mensageiro.enviarTexto(produtor.getTelefone(), texto));
    }

    /** Resposta para um número que não está cadastrado (não existe Produtor). */
    public void textoParaNumeroDesconhecido(String telefone, String texto) {
        registrar(null, telefone, Direcao.SAIDA, TipoMensagem.TEXTO, texto, null, null, null);
        tentar(() -> mensageiro.enviarTexto(telefone, texto));
    }

    public void botoes(Produtor produtor, String texto, List<Opcao> botoes) {
        registrar(produtor, produtor.getTelefone(), Direcao.SAIDA, TipoMensagem.BOTOES, texto, botoes, null, null);
        tentar(() -> mensageiro.enviarBotoes(produtor.getTelefone(), texto, botoes));
    }

    public void lista(Produtor produtor, String texto, String tituloDoBotao, List<Opcao> itens) {
        registrar(produtor, produtor.getTelefone(), Direcao.SAIDA, TipoMensagem.LISTA, texto, itens, null, null);
        tentar(() -> mensageiro.enviarLista(produtor.getTelefone(), texto, tituloDoBotao, itens));
    }

    /**
     * Lembrete: dentro da janela de 24 horas manda texto normal; fora dela a Meta só aceita template aprovado.
     */
    public void lembrete(Produtor produtor, String nomeDoTemplate, List<String> parametros, String textoCompleto) {
        if (dentroDaJanelaDe24h(produtor.getTelefone())) {
            texto(produtor, textoCompleto);
        } else {
            registrar(produtor, produtor.getTelefone(), Direcao.SAIDA, TipoMensagem.TEMPLATE, textoCompleto, null, null, null);
            tentar(() -> mensageiro.enviarTemplate(produtor.getTelefone(), nomeDoTemplate, parametros));
        }
    }

    /** O produtor escreveu para nós nas últimas 24 horas? Se sim, podemos mandar texto livre. */
    public boolean dentroDaJanelaDe24h(String telefone) {
        return historico.findFirstByTelefoneAndDirecaoOrderByCriadaEmDesc(telefone, Direcao.ENTRADA)
                .map(m -> Duration.between(m.getCriadaEm(), Relogio.agora()).toHours() < 24)
                .orElse(false);
    }

    public void registrarEntrada(Produtor produtor, String telefone, TipoMensagem tipo, String texto,
                                 String urlMidia, String idExterno) {
        registrar(produtor, telefone, Direcao.ENTRADA, tipo, texto, null, urlMidia, idExterno);
    }

    private void registrar(Produtor produtor, String telefone, Direcao direcao, TipoMensagem tipo, String texto,
                           List<Opcao> opcoes, String urlMidia, String idExterno) {
        MensagemLog msg = new MensagemLog();
        msg.setProdutor(produtor);
        msg.setTelefone(telefone);
        msg.setDirecao(direcao);
        msg.setTipo(tipo);
        msg.setTexto(texto != null && texto.length() > 2000 ? texto.substring(0, 2000) : texto);
        if (opcoes != null) {
            msg.setOpcoes(opcoes.stream().map(o -> o.id() + "|" + o.titulo()).collect(Collectors.joining("\n")));
        }
        msg.setUrlMidia(urlMidia);
        msg.setIdExterno(idExterno);
        msg.setCriadaEm(LocalDateTime.now(Relogio.FUSO));
        historico.save(msg);
    }

    private void tentar(Runnable envio) {
        try {
            envio.run();
        } catch (Exception e) {
            log.error("Não consegui enviar a mensagem pelo WhatsApp: {}", e.getMessage());
        }
    }
}
