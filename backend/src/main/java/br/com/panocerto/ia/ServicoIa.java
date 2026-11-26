package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.robo.ParserTexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Porta de entrada da IA. A IA só faz três coisas: contar insetos na foto, transcrever o áudio
 * e extrair números de texto livre. Quem decide aplicar ou não é a RegraMip, nunca a IA.
 *
 * Sem chave, ou se a IA falhar, entra o "modo demo" (números fictícios, avisados na resposta). Mas o demo só vale com o simulador:
 * se o WhatsApp real estiver ligado, nunca inventamos números para um produtor de verdade.
 */
@Service
public class ServicoIa {

    private static final Logger log = LoggerFactory.getLogger(ServicoIa.class);

    /** Resultado da contagem de uma foto. Confiança de 0 a 1. */
    public record Contagem(int percevejos, int lagartas, double confianca, boolean simulada) {
        public Contagem(int percevejos, int lagartas, double confianca) {
            this(percevejos, lagartas, confianca, false);
        }
    }

    /** O texto ouvido no áudio. "simulada" quando veio do modo de demonstração. */
    public record Transcricao(String texto, boolean simulada) {
    }

    private final Configuracao config;
    private final ClienteClaude claude;
    private final ClienteOpenAi openai;
    private final ClienteTranscricao transcricao;
    private final IaDemo demo = new IaDemo();

    public ServicoIa(Configuracao config, ClienteClaude claude, ClienteOpenAi openai, ClienteTranscricao transcricao) {
        this.config = config;
        this.claude = claude;
        this.openai = openai;
        this.transcricao = transcricao;
    }

    /** Se a IA não responder neste prazo, a mensagem segue com a demonstração (a banca não espera). */
    static final int PRAZO_SEGUNDOS = 9;
    private final ExecutorService chamadas = Executors.newCachedThreadPool();

    /** Roda a chamada à IA com prazo. Passou do prazo: desiste e lança erro, para cair no plano B. */
    private <T> T comPrazo(Callable<T> chamada) throws Exception {
        Future<T> futuro = chamadas.submit(chamada);
        try {
            return futuro.get(PRAZO_SEGUNDOS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new IllegalStateException("a IA passou de " + PRAZO_SEGUNDOS + " s");
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception causa ? causa : e;
        }
    }

    /** Ligado pelo técnico no painel quando a internet ou a IA atrapalham a apresentação. */
    private volatile boolean planoB;

    public boolean planoB() {
        return planoB;
    }

    public void definirPlanoB(boolean ativo) {
        this.planoB = ativo;
        log.info("Plano B da IA {}.", ativo ? "ligado" : "desligado");
    }

    public Contagem contarInsetos(byte[] imagem, String tipoMime) {
        if (leitorConfigurado() && !planoB) {
            try {
                return comPrazo(() -> leitor().contarFoto(imagem, tipoMime));
            } catch (Exception e) {
                log.warn("A IA falhou na foto ({}). Usando a demonstração.", e.getMessage());
            }
        }
        exigirDemoPermitido("Ainda não consigo ler fotos. Mande a contagem por texto, assim: 2 percevejos 3 lagartas 10%");
        return demo.contarInsetos(imagem);
    }

    /**
     * @param dica o que o navegador entendeu da fala (Web Speech API), ou null. Usada só quando
     *             a IA não está disponível: é mais fiel que a frase pronta da demonstração.
     */
    public Transcricao transcrever(byte[] audio, String tipoMime, String dica) {
        if (config.openaiConfigurado() && !planoB) {
            try {
                return new Transcricao(comPrazo(() -> transcricao.transcrever(audio, tipoMime)), false);
            } catch (Exception e) {
                log.warn("A IA falhou no áudio ({}). Usando a demonstração.", e.getMessage());
            }
        }
        exigirDemoPermitido("Ainda não consigo ouvir áudios. Mande a contagem por texto, assim: 2 percevejos 3 lagartas 10%");
        if (dica != null && !dica.isBlank()) {
            return new Transcricao(dica.strip(), true);
        }
        return new Transcricao(demo.transcrever(audio), true);
    }

    /** IA quando há chave; se ela falhar, cai no parser de texto (que não inventa nada). */
    public ParserTexto.Numeros extrairNumeros(String texto) {
        if (leitorConfigurado() && !planoB) {
            try {
                return comPrazo(() -> leitor().extrairNumeros(texto));
            } catch (Exception e) {
                log.warn("A IA não conseguiu extrair os números ({}). Usando o parser de texto.", e.getMessage());
            }
        }
        return ParserTexto.interpretar(texto);
    }

    /** Foto ou áudio fictícios? Sem a chave, algum dos dois está em demonstração. */
    public boolean emDemonstracao() {
        return planoB || !leitorConfigurado() || !config.openaiConfigurado();
    }

    private boolean leitorConfigurado() {
        return config.claudeConfigurado() || config.openaiConfigurado();
    }

    /** Claude tem preferência quando as duas chaves existem. */
    private ClienteIaTexto leitor() {
        return config.claudeConfigurado() ? claude : openai;
    }

    private void exigirDemoPermitido(String mensagemAmigavel) {
        if (config.whatsappConfigurado()) {
            throw new IaIndisponivelException(mensagemAmigavel);
        }
    }
}
