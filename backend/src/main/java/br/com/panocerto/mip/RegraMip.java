package br.com.panocerto.mip;

import br.com.panocerto.modelo.Decisao;
import br.com.panocerto.modelo.Finalidade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Regra do MIP (Manejo Integrado de Pragas) para soja, pelo método do pano de batida.
 *
 * Esta classe é a única que decide "aplica ou não aplica". Ela não usa banco, Spring nem IA:
 * recebe os pontos e os níveis e devolve sempre o mesmo resultado para a mesma entrada.
 * Valores a confirmar com agrônomo.
 */
public class RegraMip {

    /** Mínimo de pontos (amostras) por monitoramento. */
    public static final int MINIMO_PONTOS = 3;

    public static final String MSG_ACIMA = "Aplique só neste talhão, com produto seletivo.";
    public static final String MSG_PERTO = "Não aplique ainda. Volte a bater o pano em 3 dias.";
    public static final String MSG_ABAIXO = "Não precisa aplicar. Volte a monitorar em 7 dias.";

    /** Um ponto de pano de batida. */
    public record Ponto(int percevejos, int lagartas, double desfolha) {
    }

    /** Os níveis de ação. perto = percentual do nível (ex.: 70) em que o talhão passa a "perto". */
    public record Niveis(double percevejoGrao, double percevejoSemente, double lagarta,
                         double desfolhaVegetativa, double desfolhaReprodutiva, double perto) {

        /** Valores padrão da Embrapa / IDR-Paraná. */
        public static Niveis padrao() {
            return new Niveis(2, 1, 20, 30, 15, 70);
        }
    }

    /** Resultado da decisão: a categoria, as médias, a mensagem e os motivos. */
    public record Resultado(Decisao decisao, double mediaPercevejos, double mediaLagartas,
                            double mediaDesfolha, String mensagem, List<String> motivos) {
    }

    /** Os níveis que valem para um talhão. percevejo é null quando ainda não conta (antes do R3). */
    public record Limites(Double percevejo, double lagarta, double desfolha) {
    }

    private final Niveis niveis;

    public Limites limites(String estadio, Finalidade finalidade) {
        Double percevejo = !Estadio.chegouNoR3(estadio) ? null
                : finalidade == Finalidade.SEMENTE ? niveis.percevejoSemente() : niveis.percevejoGrao();
        double desfolha = Estadio.reprodutivo(estadio) ? niveis.desfolhaReprodutiva() : niveis.desfolhaVegetativa();
        return new Limites(percevejo, niveis.lagarta(), desfolha);
    }

    public RegraMip(Niveis niveis) {
        this.niveis = niveis;
    }

    public Resultado decidir(List<Ponto> pontos, String estadio, Finalidade finalidade) {
        if (pontos == null || pontos.size() < MINIMO_PONTOS) {
            int faltam = MINIMO_PONTOS - (pontos == null ? 0 : pontos.size());
            return new Resultado(Decisao.FALTA_DADO, 0, 0, 0,
                    "Faltam " + faltam + " ponto(s). Bata o pano em outros lugares do talhão.", List.of());
        }

        double mediaPercevejos = pontos.stream().mapToInt(Ponto::percevejos).average().orElse(0);
        double mediaLagartas = pontos.stream().mapToInt(Ponto::lagartas).average().orElse(0);
        double mediaDesfolha = pontos.stream().mapToDouble(Ponto::desfolha).average().orElse(0);

        boolean percevejoConta = Estadio.chegouNoR3(estadio);
        double nivelPercevejo = finalidade == Finalidade.SEMENTE ? niveis.percevejoSemente() : niveis.percevejoGrao();
        double nivelDesfolha = Estadio.reprodutivo(estadio) ? niveis.desfolhaReprodutiva() : niveis.desfolhaVegetativa();

        List<String> motivos = new ArrayList<>();
        if (!percevejoConta) {
            motivos.add("Percevejo ignorado: o estádio " + estadio + " é anterior ao R3.");
        }

        // Compara cada média com o nível: ACIMA se bateu o nível, PERTO se chegou a 70% dele.
        Decisao decisao = Decisao.ABAIXO;
        decisao = pior(decisao, comparar("Percevejos", mediaPercevejos, nivelPercevejo, "por pano", percevejoConta, motivos));
        decisao = pior(decisao, comparar("Lagartas", mediaLagartas, niveis.lagarta(), "por pano", true, motivos));
        decisao = pior(decisao, comparar("Desfolha", mediaDesfolha, nivelDesfolha, "%", true, motivos));

        return new Resultado(decisao, mediaPercevejos, mediaLagartas, mediaDesfolha, mensagem(decisao), motivos);
    }

    /** Texto fixo de cada decisão (o mesmo no WhatsApp, no painel e no CSV). */
    public static String mensagem(Decisao decisao) {
        return switch (decisao) {
            case ACIMA -> MSG_ACIMA;
            case PERTO -> MSG_PERTO;
            case ABAIXO -> MSG_ABAIXO;
            default -> "Faltam pontos para decidir.";
        };
    }

    /** Compara uma média com o nível e anota o motivo. Se não "conta", não influencia a decisão. */
    private Decisao comparar(String nome, double media, double nivel, String unidade, boolean conta, List<String> motivos) {
        if (!conta) {
            return Decisao.ABAIXO;
        }
        double limitePerto = nivel * niveis.perto() / 100.0;
        Decisao resultado = Decisao.ABAIXO;
        if (media >= nivel - 1e-9) {
            resultado = Decisao.ACIMA;
        } else if (media >= limitePerto - 1e-9) {
            resultado = Decisao.PERTO;
        }
        motivos.add(nome + ": média " + formatar(media) + " (nível " + formatar(nivel) + " " + unidade + ")");
        return resultado;
    }

    /** ACIMA vale mais que PERTO, que vale mais que ABAIXO. */
    private Decisao pior(Decisao a, Decisao b) {
        return a.ordinal() < b.ordinal() ? a : b;
    }

    private String formatar(double valor) {
        return String.format(Locale.of("pt", "BR"), "%.1f", valor);
    }
}
