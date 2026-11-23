package br.com.panocerto.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mensageiro de mentira, usado quando não há chaves da Meta no .env.
 * Não envia nada para fora: as mensagens aparecem no histórico (MensagemLog),
 * que é o que a tela do simulador e a tela "Conversas" leem.
 */
public class MensageiroSimulador implements MensageiroWhatsApp {

    private static final Logger log = LoggerFactory.getLogger(MensageiroSimulador.class);

    @Override
    public void enviarTexto(String telefone, String texto) {
        log.info("[simulador] para {}: {}", telefone, texto);
    }

    @Override
    public void enviarBotoes(String telefone, String texto, List<Opcao> botoes) {
        log.info("[simulador] para {}: {} {}", telefone, texto, botoes);
    }

    @Override
    public void enviarLista(String telefone, String texto, String tituloDoBotao, List<Opcao> itens) {
        log.info("[simulador] para {}: {} {}", telefone, texto, itens);
    }

    @Override
    public void enviarTemplate(String telefone, String nomeDoTemplate, List<String> parametros) {
        log.info("[simulador] template {} para {}: {}", nomeDoTemplate, telefone, parametros);
    }

    @Override
    public Midia baixarMidia(String idDaMidia) {
        // No simulador a mídia já chega junto com a mensagem; ninguém precisa baixar.
        throw new UnsupportedOperationException("O simulador não baixa mídia da Meta.");
    }

    @Override
    public String modo() {
        return "simulador";
    }
}
