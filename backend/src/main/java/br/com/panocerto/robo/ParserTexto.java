package br.com.panocerto.robo;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Entende textos como "2 percevejos 3 lagartas 10%" ou "dois percevejo e quatro lagarta"
 * sem precisar de IA. Só usa expressões regulares e uma lista de números por extenso.
 */
public final class ParserTexto {

    /** Números achados no texto. Cada campo é null quando não apareceu. */
    public record Numeros(Integer percevejos, Integer lagartas, Integer desfolha) {

        /** Achou pelo menos uma contagem de inseto? */
        public boolean temContagem() {
            return percevejos != null || lagartas != null;
        }
    }

    private static final Map<String, String> NUMEROS_POR_EXTENSO = new LinkedHashMap<>();

    static {
        // Os mais longos primeiro, para "dezesseis" não virar "dez" + "esseis".
        String[][] pares = {
                {"dezesseis", "16"}, {"dezessete", "17"}, {"dezoito", "18"}, {"dezenove", "19"},
                {"quatorze", "14"}, {"catorze", "14"}, {"quarenta", "40"}, {"cinquenta", "50"},
                {"quinze", "15"}, {"trinta", "30"}, {"vinte", "20"}, {"treze", "13"}, {"doze", "12"},
                {"onze", "11"}, {"dez", "10"}, {"nove", "9"}, {"oito", "8"}, {"sete", "7"}, {"seis", "6"},
                {"cinco", "5"}, {"quatro", "4"}, {"tres", "3"}, {"dois", "2"}, {"duas", "2"},
                {"uma", "1"}, {"um", "1"}, {"nenhuma", "0"}, {"nenhum", "0"}, {"zero", "0"}};
        for (String[] par : pares) {
            NUMEROS_POR_EXTENSO.put(par[0], par[1]);
        }
    }

    private static final Pattern PERCEVEJO_COM_NUMERO_ANTES = Pattern.compile("(\\d{1,3})\\s*percevejos?");
    private static final Pattern PERCEVEJO_COM_DOIS_PONTOS = Pattern.compile("percevejos?\\s*[:=]\\s*(\\d{1,3})");
    private static final Pattern LAGARTA_COM_NUMERO_ANTES = Pattern.compile("(\\d{1,3})\\s*lagartas?");
    private static final Pattern LAGARTA_COM_DOIS_PONTOS = Pattern.compile("lagartas?\\s*[:=]\\s*(\\d{1,3})");
    private static final Pattern DESFOLHA_COM_PORCENTO = Pattern.compile("(\\d{1,3})(?:[.,]\\d+)?\\s*(?:%|por\\s*cento)");
    private static final Pattern DESFOLHA_COM_PALAVRA = Pattern.compile("desfolha\\w*\\s*[:=]?\\s*(?:de\\s*)?(\\d{1,3})");
    private static final Pattern NUMERO_SOLTO = Pattern.compile("\\d{1,3}");

    private ParserTexto() {
    }

    /** Deixa o texto em minúsculas, sem acento, e troca "dois" por "2". */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sem = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        for (Map.Entry<String, String> numero : NUMEROS_POR_EXTENSO.entrySet()) {
            sem = sem.replaceAll("\\b" + numero.getKey() + "\\b", numero.getValue());
        }
        return sem;
    }

    /** Procura percevejos, lagartas e desfolha num texto livre. */
    public static Numeros interpretar(String texto) {
        String t = normalizar(texto);
        // A forma explícita ("percevejos: 2") vence a forma solta ("2 percevejos"), para não confundir
        // "percevejos: 2 lagartas: 5" com "2 lagartas".
        Integer percevejos = primeiroNumero(t, PERCEVEJO_COM_DOIS_PONTOS, PERCEVEJO_COM_NUMERO_ANTES);
        Integer lagartas = primeiroNumero(t, LAGARTA_COM_DOIS_PONTOS, LAGARTA_COM_NUMERO_ANTES);
        Integer desfolha = primeiroNumero(t, DESFOLHA_COM_PORCENTO, DESFOLHA_COM_PALAVRA);
        if (desfolha != null && desfolha > 100) {
            desfolha = null;
        }
        // Falou só de um inseto: o outro foi zero.
        if (percevejos != null && lagartas == null) {
            lagartas = 0;
        } else if (lagartas != null && percevejos == null) {
            percevejos = 0;
        }
        return new Numeros(percevejos, lagartas, desfolha);
    }

    /**
     * Resposta curta à pergunta "quanto das folhas está comido?": "10", "10%", "20%+", "dez por cento".
     * Devolve null se o texto não for só uma porcentagem.
     */
    public static Integer interpretarDesfolha(String texto) {
        String t = normalizar(texto);
        if (t.contains("percevejo") || t.contains("lagarta")) {
            return null;
        }
        Matcher m = NUMERO_SOLTO.matcher(t);
        if (!m.find()) {
            return null;
        }
        int valor = Integer.parseInt(m.group());
        if (m.find() || valor > 100) {   // dois números ou fora de 0 a 100: não é uma resposta simples
            return null;
        }
        return valor;
    }

    /** Normaliza um comando: "  Apliquei! " vira "apliquei". */
    public static String comando(String texto) {
        return normalizar(texto).replaceAll("[^a-z ]", "").trim();
    }

    private static Integer primeiroNumero(String texto, Pattern principal, Pattern alternativo) {
        Matcher m = principal.matcher(texto);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        m = alternativo.matcher(texto);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }
}
