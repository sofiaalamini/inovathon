package br.com.panocerto.robo;

import br.com.panocerto.whatsapp.MensageiroWhatsApp.Midia;

/**
 * Uma mensagem que chegou do produtor, já em formato simples.
 * O webhook real e o simulador montam esta mesma mensagem e entregam ao mesmo ServicoRobo.
 *
 * @param idMidia  id da mídia na Meta (webhook real); o robô baixa
 * @param midia    bytes já em mãos (simulador); nesse caso não precisa baixar
 * @param botaoId  id do botão ou item de lista que o produtor tocou
 * @param idExterno id da mensagem na Meta, para ignorar mensagens repetidas
 */
public record MensagemEntrada(String telefone, Tipo tipo, String texto, String botaoId,
                              String idMidia, Midia midia, String idExterno) {

    public enum Tipo { TEXTO, IMAGEM, AUDIO, BOTAO, OUTRO }

    public static MensagemEntrada texto(String telefone, String texto) {
        return new MensagemEntrada(telefone, Tipo.TEXTO, texto, null, null, null, null);
    }
}
