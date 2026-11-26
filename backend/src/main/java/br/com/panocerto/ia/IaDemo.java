package br.com.panocerto.ia;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;

/**
 * "Modo demo" da IA: usado quando não há chave. Devolve números fictícios, mas plausíveis,
 * sempre os mesmos para a mesma foto. Assim o sistema roda inteiro sem nenhuma chave.
 */
public class IaDemo {

    private static final String[] FALAS = {
            "bati três vez, deu dois percevejo e umas quatro lagarta",
            "fiz o pano agora, tinha um percevejo e três lagarta",
            "bati o pano, deu três percevejo e duas lagarta"};

    /** Ilustrações de exemplo da página da banca (SHA-256): a resposta bate com o desenho. */
    private static final Map<String, int[]> EXEMPLOS = Map.of(
            "6e9c18b7cbe76793f46abf13dbc88ad164631fcdf0a3bd479a40b5ad7595445e", new int[]{3, 4},
            "757aaabdca5548e700c95b9cdf41afd4c7b8519e2089067e6b79b210b93cfa9d", new int[]{2, 3},
            "cee42a607ae0b42fee7d8832328e89e0c79b8714f7d1465b888ce2140e2c1cd2", new int[]{4, 2});

    public ServicoIa.Contagem contarInsetos(byte[] imagem) {
        int[] exemplo = EXEMPLOS.get(sha256(imagem));
        if (exemplo != null) {
            return new ServicoIa.Contagem(exemplo[0], exemplo[1], 0.9, true);
        }
        int h = Arrays.hashCode(imagem) & 0x7fffffff;
        return new ServicoIa.Contagem(h % 4, (h / 7) % 6, 0.5, true);   // não sabemos o que tem na foto: o produtor confirma
    }

    private static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String transcrever(byte[] audio) {
        int h = Arrays.hashCode(audio) & 0x7fffffff;
        return FALAS[h % FALAS.length];
    }
}
