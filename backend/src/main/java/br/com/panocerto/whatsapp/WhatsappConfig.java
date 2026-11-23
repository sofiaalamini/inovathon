package br.com.panocerto.whatsapp;

import br.com.panocerto.config.Configuracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Escolhe o mensageiro: sem chaves da Meta no .env, usa o simulador automaticamente. */
@Configuration
public class WhatsappConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsappConfig.class);

    @Bean
    public MensageiroWhatsApp mensageiroWhatsApp(Configuracao config) {
        if (config.whatsappConfigurado()) {
            log.info("WhatsApp: usando a Cloud API da Meta.");
            return new MensageiroCloudApi(config);
        }
        log.info("WhatsApp: sem chaves da Meta no .env, usando o SIMULADOR.");
        return new MensageiroSimulador();
    }
}
