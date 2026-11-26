package br.com.panocerto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Todas as configurações do PanoCerto (vêm do application.yml, que lê o .env). */
@ConfigurationProperties(prefix = "panocerto")
public record Configuracao(
        String jwtSecret,
        int jwtHoras,
        String anthropicApiKey,
        String anthropicModel,
        String openaiApiKey,
        String whatsappToken,
        String whatsappPhoneNumberId,
        String whatsappVerifyToken,
        String whatsappAppSecret,
        String whatsappTemplateLembrete,
        double custoAplicacaoHa,
        String pastaUploads,
        boolean carregarDadosExemplo,
        String origensPermitidas,
        String openaiModel,
        String openaiModeloAudio,
        boolean modoBanca,
        String senhaPainel) {

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    /** O WhatsApp real só é usado se o token e o número da Meta estiverem no .env. */
    public boolean whatsappConfigurado() {
        return preenchido(whatsappToken) && preenchido(whatsappPhoneNumberId);
    }

    public boolean claudeConfigurado() {
        return preenchido(anthropicApiKey);
    }

    public boolean openaiConfigurado() {
        return preenchido(openaiApiKey);
    }
}
