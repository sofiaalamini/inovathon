package br.com.panocerto.ia;

import java.util.Arrays;

/**
 * "Modo demo" da IA: usado quando não há chave. Devolve números fictícios, mas plausíveis,
 * sempre os mesmos para a mesma foto. Assim o sistema roda inteiro sem nenhuma chave.
 */
public class IaDemo {

    private static final String[] FALAS = {
            "bati três vez, deu dois percevejo e umas quatro lagarta",
            "fiz o pano agora, tinha um percevejo e três lagarta",
            "bati o pano, deu três percevejo e duas lagarta"};

    public ServicoIa.Contagem contarInsetos(byte[] imagem) {
        int h = Arrays.hashCode(imagem) & 0x7fffffff;
        return new ServicoIa.Contagem(h % 4, (h / 7) % 6, 0.9);
    }

    public String transcrever(byte[] audio) {
        int h = Arrays.hashCode(audio) & 0x7fffffff;
        return FALAS[h % FALAS.length];
    }
}
