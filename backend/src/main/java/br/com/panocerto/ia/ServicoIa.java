package br.com.panocerto.ia;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.robo.ParserTexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Porta de entrada da IA. A IA só faz três coisas: contar insetos na foto, transcrever o áudio
 * e extrair números de texto livre. Quem decide aplicar ou não é a RegraMip, nunca a IA.
 *
 * Sem chave, entra o "modo demo" (números fictícios). Mas o demo só vale com o simulador:
 * se o WhatsApp real estiver ligado, nunca inventamos números para um produtor de verdade.
 */
@Service
public class ServicoIa {

    private static final Logger log = LoggerFactory.getLogger(ServicoIa.class);

    /** Resultado da contagem de uma foto. Confiança de 0 a 1. */
    public record Contagem(int percevejos, int lagartas, double confianca) {
    }

    private final Configuracao config;
    private final ClienteClaude claude;
    private final ClienteWhisper whisper;
    private final IaDemo demo = new IaDemo();

    public ServicoIa(Configuracao config, ClienteClaude claude, ClienteWhisper whisper) {
        this.config = config;
        this.claude = claude;
        this.whisper = whisper;
    }

    public Contagem contarInsetos(byte[] imagem, String tipoMime) {
        if (config.claudeConfigurado()) {
            return claude.contarFoto(imagem, tipoMime);
        }
        exigirDemoPermitido("Ainda não consigo ler fotos. Mande a contagem por texto, assim: 2 percevejos 3 lagartas 10%");
        return demo.contarInsetos(imagem);
    }

    public String transcrever(byte[] audio, String tipoMime) {
        if (config.whisperConfigurado()) {
            return whisper.transcrever(audio, tipoMime);
        }
        exigirDemoPermitido("Ainda não consigo ouvir áudios. Mande a contagem por texto, assim: 2 percevejos 3 lagartas 10%");
        return demo.transcrever(audio);
    }

    /** Claude quando há chave; se ele falhar, cai no parser de texto (que não inventa nada). */
    public ParserTexto.Numeros extrairNumeros(String texto) {
        if (config.claudeConfigurado()) {
            try {
                return claude.extrairNumeros(texto);
            } catch (Exception e) {
                log.warn("Claude não conseguiu extrair os números ({}). Usando o parser de texto.", e.getMessage());
            }
        }
        return ParserTexto.interpretar(texto);
    }

    public boolean claudeAtivo() {
        return config.claudeConfigurado();
    }

    public boolean whisperAtivo() {
        return config.whisperConfigurado();
    }

    private void exigirDemoPermitido(String mensagemAmigavel) {
        if (config.whatsappConfigurado()) {
            throw new IaIndisponivelException(mensagemAmigavel);
        }
    }
}
