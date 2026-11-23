package br.com.panocerto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AsyncConfig {

    /**
     * Fila do robô: uma thread só, para as mensagens de um produtor serem tratadas
     * na ordem em que chegaram (a foto antes da resposta da desfolha, por exemplo).
     */
    @Bean(name = "filaDoRobo")
    public Executor filaDoRobo() {
        return Executors.newSingleThreadExecutor(r -> new Thread(r, "fila-do-robo"));
    }
}
