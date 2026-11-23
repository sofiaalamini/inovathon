package br.com.panocerto.mip;

import java.util.regex.Pattern;

/**
 * Estádio fenológico da soja, guardado como texto: V1..V19 (vegetativo) e R1..R8 (reprodutivo).
 * Esta classe só responde perguntas sobre esse texto.
 */
public final class Estadio {

    private static final Pattern VALIDO = Pattern.compile("^(V([1-9]|1[0-9])|R[1-8])$");

    private Estadio() {
    }

    public static boolean valido(String estadio) {
        return estadio != null && VALIDO.matcher(estadio).matches();
    }

    public static boolean reprodutivo(String estadio) {
        return estadio != null && estadio.startsWith("R");
    }

    /** O percevejo só entra na decisão a partir do R3. */
    public static boolean chegouNoR3(String estadio) {
        if (!valido(estadio) || !reprodutivo(estadio)) {
            return false;
        }
        return Integer.parseInt(estadio.substring(1)) >= 3;
    }
}
