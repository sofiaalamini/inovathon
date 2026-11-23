package br.com.panocerto.config;

import java.time.LocalDateTime;
import java.time.ZoneId;

/** Hora de Brasília para todo o sistema, não importa onde o servidor esteja. */
public final class Relogio {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Relogio() {
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(FUSO);
    }
}
