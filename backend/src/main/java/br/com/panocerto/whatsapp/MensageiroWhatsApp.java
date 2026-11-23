package br.com.panocerto.whatsapp;

import java.util.List;

/**
 * Tudo que o sistema precisa fazer com o WhatsApp. Existem duas implementações:
 * a real (Cloud API da Meta) e o simulador. O resto do código só conhece esta interface.
 */
public interface MensageiroWhatsApp {

    /** Botão de resposta rápida ou item de lista. O "id" volta quando o produtor toca. */
    record Opcao(String id, String titulo) {
    }

    /** Foto ou áudio baixado. */
    record Midia(byte[] bytes, String tipoMime) {
    }

    void enviarTexto(String telefone, String texto);

    /** Até 3 botões (limite do WhatsApp). */
    void enviarBotoes(String telefone, String texto, List<Opcao> botoes);

    /** Até 10 itens (limite do WhatsApp). */
    void enviarLista(String telefone, String texto, String tituloDoBotao, List<Opcao> itens);

    /** Mensagem de template aprovado pela Meta (obrigatória fora da janela de 24 horas). */
    void enviarTemplate(String telefone, String nomeDoTemplate, List<String> parametros);

    /** Baixa uma foto ou áudio recebido, pelo id que a Meta mandou. */
    Midia baixarMidia(String idDaMidia);

    /** Nome curto para mostrar no painel: "simulador" ou "cloud-api". */
    String modo();
}
