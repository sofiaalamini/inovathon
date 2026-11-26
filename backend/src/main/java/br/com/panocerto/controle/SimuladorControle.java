package br.com.panocerto.controle;

import br.com.panocerto.modelo.Produtor;
import br.com.panocerto.robo.MensagemEntrada;
import br.com.panocerto.robo.ServicoRobo;
import br.com.panocerto.servico.Acesso;
import br.com.panocerto.whatsapp.MensageiroWhatsApp.Midia;
import org.springframework.http.MediaType;
import br.com.panocerto.repositorio.RepositorioMensagemLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Tela "Simulador de WhatsApp". Monta uma MensagemEntrada igual à do webhook real
 * e entrega ao MESMO ServicoRobo. A resposta do robô aparece no histórico da conversa.
 */
@RestController
@RequestMapping("/api/simulador")
public class SimuladorControle {

    /** Número fictício (DDI 00) que nunca é cadastrado, usado para simular um desconhecido. */
    public static final String NUMERO_DESCONHECIDO = "5500000000000";

    private final ServicoRobo robo;
    private final RepositorioMensagemLog historico;
    private final Acesso acesso;

    public SimuladorControle(ServicoRobo robo, RepositorioMensagemLog historico, Acesso acesso) {
        this.robo = robo;
        this.historico = historico;
        this.acesso = acesso;
    }

    /** Mensagens trocadas com o número fictício desconhecido (para a tela do simulador). */
    @GetMapping("/desconhecido/mensagens")
    public List<ConversaControle.MensagemDto> mensagensDoDesconhecido() {
        return historico.findByTelefoneOrderByCriadaEmAsc(NUMERO_DESCONHECIDO).stream()
                .map(ConversaControle::paraDto).toList();
    }

    /**
     * @param produtorId   quem está "mandando" a mensagem
     * @param desconhecido true para testar um número não cadastrado (sempre o número fictício fixo,
     *                     para o técnico nunca poder se passar por um telefone de verdade)
     * @param tipo         TEXTO, IMAGEM, AUDIO ou BOTAO
     */
    @PostMapping(value = "/mensagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> enviar(@RequestParam(required = false) Long produtorId,
                                      @RequestParam(required = false, defaultValue = "false") boolean desconhecido,
                                      @RequestParam String tipo,
                                      @RequestParam(required = false) String texto,
                                      @RequestParam(required = false) String botaoId,
                                      @RequestParam(required = false) MultipartFile arquivo) throws IOException {
        String numero;
        if (produtorId != null) {
            Produtor produtor = acesso.produtor(produtorId);
            numero = produtor.getTelefone();
        } else if (desconhecido) {
            numero = NUMERO_DESCONHECIDO;
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "Escolha um produtor.");
        }

        robo.processar(montar(numero, tipo, texto, botaoId, arquivo));
        return Map.of("ok", true);
    }

    /** Valida o que chegou da tela e monta a mensagem (usado também pela página da banca). */
    static MensagemEntrada montar(String numero, String tipo, String texto, String botaoId, MultipartFile arquivo)
            throws IOException {
        MensagemEntrada.Tipo tipoEntrada;
        try {
            tipoEntrada = MensagemEntrada.Tipo.valueOf(tipo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Tipo de mensagem inválido.");
        }

        Midia midia = null;
        if (tipoEntrada == MensagemEntrada.Tipo.IMAGEM || tipoEntrada == MensagemEntrada.Tipo.AUDIO) {
            if (arquivo == null || arquivo.isEmpty()) {
                throw new ResponseStatusException(BAD_REQUEST, "Envie um arquivo.");
            }
            String mime = arquivo.getContentType() == null ? "" : arquivo.getContentType();
            boolean tipoCerto = tipoEntrada == MensagemEntrada.Tipo.IMAGEM
                    ? mime.startsWith("image/")
                    : mime.startsWith("audio/") || mime.startsWith("video/webm");
            if (!tipoCerto) {
                throw new ResponseStatusException(BAD_REQUEST, "O arquivo não é do tipo esperado (" + tipo + ").");
            }
            midia = new Midia(arquivo.getBytes(), mime);
        } else if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Escreva uma mensagem.");
        }

        return new MensagemEntrada(numero, tipoEntrada, texto, botaoId, null, midia, null);
    }
}
