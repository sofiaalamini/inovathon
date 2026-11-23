package br.com.panocerto.servico;

import java.util.ArrayList;
import java.util.List;

/** Cuida do formato do telefone: guardamos só os dígitos, com o DDI (ex.: 5545999990001). */
public final class Telefone {

    private Telefone() {
    }

    /** Tira +, espaços, parênteses e traços. */
    public static String normalizar(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D", "");
    }

    /** Formato internacional: de 10 a 15 dígitos, incluindo o DDI. */
    public static boolean valido(String digitos) {
        return digitos != null && digitos.matches("\\d{10,15}");
    }

    /**
     * Celular do Brasil: a Meta às vezes manda o número sem o 9 depois do DDD.
     * Por isso procuramos o produtor com e sem esse 9.
     */
    public static List<String> variantes(String digitos) {
        List<String> lista = new ArrayList<>(List.of(digitos));
        if (digitos.startsWith("55")) {
            if (digitos.length() == 12) {
                lista.add(digitos.substring(0, 4) + "9" + digitos.substring(4));
            } else if (digitos.length() == 13 && digitos.charAt(4) == '9') {
                lista.add(digitos.substring(0, 4) + digitos.substring(5));
            }
        }
        return lista;
    }
}
