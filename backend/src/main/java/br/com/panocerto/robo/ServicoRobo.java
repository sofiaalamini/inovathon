package br.com.panocerto.robo;

import br.com.panocerto.config.Relogio;
import br.com.panocerto.ia.IaIndisponivelException;
import br.com.panocerto.ia.ServicoIa;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.RepositorioConversaEstado;
import br.com.panocerto.repositorio.RepositorioMensagemLog;
import br.com.panocerto.repositorio.RepositorioProdutor;
import br.com.panocerto.repositorio.RepositorioTalhao;
import br.com.panocerto.servico.ServicoArmazenamento;
import br.com.panocerto.servico.ServicoEnvio;
import br.com.panocerto.servico.ServicoMonitoramento;
import br.com.panocerto.servico.ServicoMonitoramento.ResultadoAmostra;
import br.com.panocerto.servico.Telefone;
import br.com.panocerto.whatsapp.MensageiroWhatsApp;
import br.com.panocerto.whatsapp.MensageiroWhatsApp.Midia;
import br.com.panocerto.whatsapp.MensageiroWhatsApp.Opcao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A conversa do robô. Recebe uma MensagemEntrada (do webhook real ou do simulador) e responde.
 *
 * Caminho de uma mensagem:
 *   1. descobre o produtor pelo telefone;
 *   2. carrega o estado da conversa (talhão escolhido, amostra esperando a desfolha...);
 *   3. trata foto, áudio, texto ou botão;
 *   4. com 3 pontos, a RegraMip decide (isso fica no ServicoMonitoramento).
 */
@Service
public class ServicoRobo {

    private static final Logger log = LoggerFactory.getLogger(ServicoRobo.class);

    /** Abaixo disso, a contagem da foto pode estar errada e o produtor confirma. */
    private static final double CONFIANCA_MINIMA = 0.6;
    /** Abaixo disso, a foto provavelmente nem é de um pano de batida. */
    private static final double CONFIANCA_PANO = 0.3;
    /** Mais que isso num pano só é raro: o produtor confirma antes de registrar. */
    static final int LIMITE_CONFERIR = 50;

    static final String MSG_NAO_CADASTRADO =
            "Olá! Seu número ainda não está cadastrado. Peça para o técnico da sua cooperativa te cadastrar.";
    static final String MSG_ERRO =
            "Tive um problema para entender essa mensagem. Tente de novo daqui a pouco. Se continuar, digite ajuda.";
    static final String MSG_AJUDA = """
            Como usar o PanoCerto:
            1. Bata o pano em pelo menos 3 pontos do talhão, em lugares diferentes.
            2. Em cada ponto, mande uma foto do pano, um áudio ou um texto. Exemplo: 2 percevejos 3 lagartas 10%
            3. Depois de 3 pontos eu digo se precisa aplicar.
            Comandos: resultado, novo, trocar, apliquei, ajuda.""";

    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioConversaEstado conversas;
    private final RepositorioMensagemLog historico;
    private final ServicoEnvio envio;
    private final ServicoIa ia;
    private final ServicoMonitoramento monitoramento;
    private final ServicoArmazenamento armazenamento;
    private final MensageiroWhatsApp mensageiro;

    public ServicoRobo(RepositorioProdutor produtores, RepositorioTalhao talhoes, RepositorioConversaEstado conversas,
                       RepositorioMensagemLog historico, ServicoEnvio envio, ServicoIa ia,
                       ServicoMonitoramento monitoramento, ServicoArmazenamento armazenamento,
                       MensageiroWhatsApp mensageiro) {
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.conversas = conversas;
        this.historico = historico;
        this.envio = envio;
        this.ia = ia;
        this.monitoramento = monitoramento;
        this.armazenamento = armazenamento;
        this.mensageiro = mensageiro;
    }

    // ------------------------------------------------------------------ entrada

    public void processar(MensagemEntrada entrada) {
        if (entrada.idExterno() != null && historico.existsByIdExterno(entrada.idExterno())) {
            return;   // a Meta reenviou a mesma mensagem
        }
        String telefone = Telefone.normalizar(entrada.telefone());
        Optional<Produtor> achado = produtores.findByTelefoneIn(Telefone.variantes(telefone)).stream().findFirst();

        // Foto e áudio: baixa (se for da Meta) e guarda no disco, para aparecer no painel.
        Midia midia = null;
        String urlMidia = null;
        try {
            midia = obterMidia(entrada);
            if (midia != null) {
                urlMidia = armazenamento.salvar(midia.bytes(), midia.tipoMime());
            }
        } catch (Exception e) {
            log.error("Falha ao obter a mídia: {}", e.getMessage());
        }

        envio.registrarEntrada(achado.orElse(null), telefone, tipoParaLog(entrada.tipo()),
                textoParaLog(entrada), urlMidia, entrada.idExterno());

        if (achado.isEmpty()) {
            envio.textoParaNumeroDesconhecido(telefone, MSG_NAO_CADASTRADO);
            return;
        }

        Produtor produtor = achado.get();
        ConversaEstado conversa = conversas.findByTelefone(produtor.getTelefone()).orElseGet(() -> {
            ConversaEstado nova = new ConversaEstado();
            nova.setTelefone(produtor.getTelefone());
            nova.setProdutor(produtor);
            return nova;
        });

        try {
            tratar(entrada, conversa, produtor, midia, urlMidia);
        } catch (IaIndisponivelException e) {
            conversa.limparPendencia();
            envio.texto(produtor, e.getMessage());
        } catch (Exception e) {
            // Nunca deixa o erro chegar na Meta nem no produtor: responde de forma amigável.
            log.error("Erro ao tratar a mensagem do produtor {}", produtor.getId(), e);
            conversa.limparPendencia();
            envio.texto(produtor, MSG_ERRO);
        }
        conversa.setAtualizadaEm(Relogio.agora());
        conversas.save(conversa);
    }

    /** Primeira mensagem de quem entra pela página da banca: explica e já pergunta o talhão. */
    public void saudar(String telefone) {
        Produtor produtor = produtores.findByTelefoneIn(List.of(telefone)).stream().findFirst().orElseThrow();
        ConversaEstado conversa = new ConversaEstado();
        conversa.setTelefone(telefone);
        conversa.setProdutor(produtor);
        perguntarTalhao(conversa, produtor, "Olá, " + produtor.getNome() + "! Vamos bater o pano em 3 pontos do talhão. "
                + "Em cada ponto, mande uma foto, um áudio ou um texto, como: 3 percevejos 4 lagartas 10%.\n");
        conversa.setAtualizadaEm(Relogio.agora());
        conversas.save(conversa);
    }

    private void tratar(MensagemEntrada entrada, ConversaEstado c, Produtor p, Midia midia, String urlMidia) {
        switch (entrada.tipo()) {
            case BOTAO -> tratarBotao(c, p, entrada.botaoId());
            case TEXTO -> tratarTexto(c, p, entrada.texto());
            case IMAGEM -> tratarFoto(c, p, midia, urlMidia);
            case AUDIO -> tratarAudio(c, p, midia, urlMidia, entrada.texto());
            default -> envio.texto(p, "Ainda não entendo esse tipo de mensagem. "
                    + "Mande uma foto do pano, um áudio ou um texto. Digite ajuda para ver como usar.");
        }
    }

    private Midia obterMidia(MensagemEntrada entrada) {
        if (entrada.midia() != null) {
            return entrada.midia();
        }
        if (entrada.idMidia() != null) {
            return mensageiro.baixarMidia(entrada.idMidia());
        }
        return null;
    }

    // ------------------------------------------------------------------ botões

    private void tratarBotao(ConversaEstado c, Produtor p, String botaoId) {
        if (botaoId == null) {
            envio.texto(p, MSG_AJUDA);
        } else if (botaoId.startsWith("TALHAO_")) {
            Talhao talhao = talhaoDoProdutor(p, botaoId.substring("TALHAO_".length()));
            if (talhao == null) {
                perguntarTalhao(c, p, "Não achei esse talhão. ");
            } else {
                escolherTalhao(c, p, talhao);
            }
        } else if (botaoId.startsWith("DESF_")) {
            if (c.getEstado() == EstadoConversa.AGUARDANDO_DESFOLHA && c.getPendentePercevejos() != null) {
                finalizarAmostra(c, p, Integer.parseInt(botaoId.substring("DESF_".length())));
            } else {
                envio.texto(p, "Esse botão já foi usado. Mande a contagem do próximo ponto.");
            }
        } else if (botaoId.equals("CONT_OK")) {
            if (c.getEstado() == EstadoConversa.CONFIRMANDO_CONTAGEM) {
                confirmarPendencia(c, p);
            } else {
                envio.texto(p, "Mande a foto, o áudio ou o texto do ponto.");
            }
        } else if (botaoId.equals("CONT_DIGITAR")) {
            envio.texto(p, c.getEstado() == EstadoConversa.CONFIRMANDO_CONTAGEM
                    ? "Corrija a contagem, assim: 2 percevejos 3 lagartas"
                    : "Conte no botão Contar ou escreva assim: 2 percevejos 3 lagartas 10%");
        } else if (botaoId.equals("FOTO_NOVA")) {
            envio.texto(p, "Mande outra foto do pano, de perto e com boa luz.");
        } else if (botaoId.equals("TROCAR")) {
            comandoTrocar(c, p);
        } else {
            envio.texto(p, MSG_AJUDA);
        }
    }

    // ------------------------------------------------------------------ texto

    private void tratarTexto(ConversaEstado c, Produtor p, String texto) {
        String comando = ParserTexto.comando(texto);
        switch (comando) {
            case "ajuda" -> { envio.texto(p, MSG_AJUDA); return; }
            case "novo" -> { comandoNovo(c, p); return; }
            case "trocar" -> { comandoTrocar(c, p); return; }
            case "resultado" -> { comandoResultado(c, p); return; }
            case "apliquei" -> { comandoApliquei(c, p); return; }
            default -> { }
        }

        if (c.getEstado() == EstadoConversa.ESCOLHENDO_TALHAO) {
            Talhao talhao = talhaoPorNomeOuNumero(p, texto);
            if (talhao != null) {
                escolherTalhao(c, p, talhao);
            } else {
                perguntarTalhao(c, p, "Não entendi qual talhão. ");
            }
            return;
        }

        if (c.getEstado() == EstadoConversa.AGUARDANDO_DESFOLHA) {
            Integer desfolha = ParserTexto.interpretarDesfolha(texto);
            if (desfolha != null) {
                finalizarAmostra(c, p, desfolha);
                return;
            }
        }

        if (c.getEstado() == EstadoConversa.CONFIRMANDO_CONTAGEM && ehConfirmacao(comando)) {
            confirmarPendencia(c, p);
            return;
        }

        String problema = ParserTexto.problema(texto);
        if (problema != null) {
            envio.texto(p, problema);
            return;
        }

        // Texto simples ("2 percevejos 3 lagartas 10%"): o parser resolve na hora. Frase solta: a IA lê.
        ParserTexto.Numeros numeros = ParserTexto.simples(texto) ? ParserTexto.interpretar(texto) : ia.extrairNumeros(texto);
        if (numeros.temContagem()) {
            receberContagem(c, p, numeros, Origem.TEXTO, null, null, "Entendi: ");
        } else if (c.getEstado() == EstadoConversa.AGUARDANDO_DESFOLHA) {
            envio.texto(p, "Quanto das folhas está comido? Responda com um número de 0 a 100, por exemplo 10.");
        } else {
            envio.texto(p, "Não consegui entender. Mande assim: 2 percevejos 3 lagartas 10%. Ou digite ajuda.");
        }
    }

    private boolean ehConfirmacao(String comando) {
        return List.of("sim", "certo", "isso", "ok", "confirmo", "correto").contains(comando);
    }

    // ------------------------------------------------------------------ foto e áudio

    private void tratarFoto(ConversaEstado c, Produtor p, Midia midia, String urlMidia) {
        if (midia == null) {
            envio.texto(p, "Não consegui abrir essa foto. Tente mandar de novo.");
            return;
        }
        Talhao talhao = talhaoOuPerguntar(c, p, "Antes da foto, preciso saber o talhão. Depois mande a foto de novo. ");
        if (talhao == null) {
            return;
        }
        ServicoIa.Contagem contagem = ia.contarInsetos(midia.bytes(), midia.tipoMime());
        if (contagem.confianca() < CONFIANCA_PANO || (contagem.percevejos() == 0 && contagem.lagartas() == 0)) {
            // Nada registrado: o produtor tenta outra foto ou conta no botão.
            envio.botoes(p, "Não parece uma foto do pano. Quer tentar de novo ou contar no botão?",
                    List.of(new Opcao("FOTO_NOVA", "Tentar de novo"), new Opcao("CONT_DIGITAR", "Contar no botão")));
            return;
        }
        guardarPendencia(c, contagem.percevejos(), contagem.lagartas(), Origem.FOTO, urlMidia, null);

        String aviso = contagem.simulada() ? " (contagem de demonstração)" : "";
        boolean muito = contagem.percevejos() > LIMITE_CONFERIR || contagem.lagartas() > LIMITE_CONFERIR;
        if (contagem.confianca() < CONFIANCA_MINIMA || muito) {
            c.setEstado(EstadoConversa.CONFIRMANDO_CONTAGEM);
            envio.botoes(p, (muito ? "" : "Não tenho certeza da foto. ") + "Contei "
                            + contar(contagem.percevejos(), contagem.lagartas()) + aviso + ". Está certo?",
                    List.of(new Opcao("CONT_OK", "Está certo"), new Opcao("CONT_DIGITAR", "Corrigir")));
            return;
        }
        perguntarDesfolha(c, p, "Contei " + contar(contagem.percevejos(), contagem.lagartas()) + aviso + ". ");
    }

    private void tratarAudio(ConversaEstado c, Produtor p, Midia midia, String urlMidia, String dicaDoNavegador) {
        if (midia == null) {
            envio.texto(p, "Não consegui abrir esse áudio. Tente gravar de novo.");
            return;
        }
        Talhao talhao = talhaoOuPerguntar(c, p, "Antes do áudio, preciso saber o talhão. Depois mande o áudio de novo. ");
        if (talhao == null) {
            return;
        }
        ServicoIa.Transcricao ouvido = ia.transcrever(midia.bytes(), midia.tipoMime(), dicaDoNavegador);
        String transcricao = ouvido.texto();
        // "Ouvi" só quando é o que a pessoa falou (IA ou navegador), nunca a frase pronta da demonstração.
        boolean ouviuDeVerdade = !ouvido.simulada() || (dicaDoNavegador != null && !dicaDoNavegador.isBlank());
        String ouvi = ouviuDeVerdade ? "Ouvi: \"" + transcricao + "\".\n" : "";

        String problema = ParserTexto.problema(transcricao);
        if (problema != null) {
            envio.texto(p, ouvi + problema);
            return;
        }
        ParserTexto.Numeros numeros = ParserTexto.simples(transcricao) || ouvido.simulada()
                ? ParserTexto.interpretar(transcricao) : ia.extrairNumeros(transcricao);
        if (!numeros.temContagem()) {
            envio.texto(p, ouvi + "Não achei a contagem. Mande de novo falando assim: dois percevejos, três lagartas.");
            return;
        }
        // Áudio sempre passa pela conferência do produtor antes de virar ponto.
        guardarPendencia(c, numeros.percevejos(), numeros.lagartas(), Origem.AUDIO, urlMidia, transcricao);
        c.setPendenteDesfolha(numeros.desfolha());
        c.setEstado(EstadoConversa.CONFIRMANDO_CONTAGEM);
        String desfolha = numeros.desfolha() == null ? "" : " e " + numeros.desfolha() + "% de desfolha";
        envio.botoes(p, ouvi + "Entendi: " + contar(numeros.percevejos(), numeros.lagartas()) + desfolha + ". Está certo?",
                List.of(new Opcao("CONT_OK", "Está certo"), new Opcao("CONT_DIGITAR", "Corrigir")));
    }

    // ------------------------------------------------------------------ amostra

    /** Já temos percevejos e lagartas. Se veio a desfolha, registra; senão pergunta. */
    private void receberContagem(ConversaEstado c, Produtor p, ParserTexto.Numeros n, Origem origem,
                                 String urlMidia, String transcricao, String prefixo) {
        Talhao talhao = talhaoOuPerguntar(c, p, "Preciso saber o talhão primeiro. Depois mande a contagem de novo. ");
        if (talhao == null) {
            return;
        }
        guardarPendencia(c, n.percevejos(), n.lagartas(), origem, urlMidia, transcricao);
        if (n.percevejos() > LIMITE_CONFERIR || n.lagartas() > LIMITE_CONFERIR) {
            c.setPendenteDesfolha(n.desfolha());
            c.setEstado(EstadoConversa.CONFIRMANDO_CONTAGEM);
            envio.botoes(p, "Contei " + contar(n.percevejos(), n.lagartas()) + " num pano só. É bastante: está certo?",
                    List.of(new Opcao("CONT_OK", "Sim, está certo"), new Opcao("CONT_DIGITAR", "Corrigir")));
            return;
        }
        String entendi = prefixo + contar(n.percevejos(), n.lagartas());
        if (n.desfolha() != null) {
            envio.texto(p, prefixo + contarComDesfolha(n.percevejos(), n.lagartas(), n.desfolha()) + ".");
            finalizarAmostra(c, p, n.desfolha());
        } else {
            perguntarDesfolha(c, p, entendi + ". ");
        }
    }

    /** O produtor confirmou a contagem: se a desfolha já veio junto, fecha o ponto; senão pergunta. */
    private void confirmarPendencia(ConversaEstado c, Produtor p) {
        if (c.getPendenteDesfolha() != null) {
            finalizarAmostra(c, p, c.getPendenteDesfolha());
        } else {
            perguntarDesfolha(c, p, "");
        }
    }

    private void perguntarDesfolha(ConversaEstado c, Produtor p, String prefixo) {
        c.setEstado(EstadoConversa.AGUARDANDO_DESFOLHA);
        envio.botoes(p, prefixo + "Quanto das folhas está comido?",
                List.of(new Opcao("DESF_0", "0%"), new Opcao("DESF_10", "10%"), new Opcao("DESF_20", "20% ou mais")));
    }

    private void guardarPendencia(ConversaEstado c, int percevejos, int lagartas, Origem origem,
                                  String urlMidia, String transcricao) {
        c.setPendentePercevejos(percevejos);
        c.setPendenteLagartas(lagartas);
        c.setPendenteOrigem(origem);
        c.setPendenteUrlMidia(urlMidia);
        c.setPendenteTranscricao(transcricao);
        c.setPendenteDesfolha(null);
    }

    /** Fecha o ponto: grava a amostra e, se completou 3 pontos, responde com a decisão. */
    private void finalizarAmostra(ConversaEstado c, Produtor p, int desfolha) {
        Talhao talhao = c.getTalhaoAtual();
        ResultadoAmostra r = monitoramento.registrarAmostra(talhao, c.getPendentePercevejos(), c.getPendenteLagartas(),
                desfolha, c.getPendenteOrigem(), c.getPendenteUrlMidia(), c.getPendenteTranscricao());
        String resumo = contarComDesfolha(c.getPendentePercevejos(), c.getPendenteLagartas(), desfolha);
        c.limparPendencia();

        if (r.resultado() == null) {
            int faltam = 3 - r.pontos();
            envio.texto(p, "Ponto " + r.pontos() + " registrado no talhão " + talhao.getNome() + ": " + resumo + ". "
                    + (faltam == 1 ? "Falta 1 ponto. " : "Faltam " + faltam + " pontos. ")
                    + "Bata o pano em outro lugar do talhão, longe do anterior.");
            return;
        }
        var res = r.resultado();
        String texto = "Monitoramento do talhão " + talhao.getNome() + " completo (3 pontos).\n"
                + "Média por pano: " + numero(res.mediaPercevejos()) + " percevejos, "
                + numero(res.mediaLagartas()) + " lagartas, desfolha " + porcento(res.mediaDesfolha()) + "%.\n"
                + res.mensagem();
        if (r.alerta() != null) {
            texto += "\nO técnico da cooperativa foi avisado e vai confirmar com você.";
        }
        envio.texto(p, texto);
    }

    // ------------------------------------------------------------------ comandos

    private void comandoNovo(ConversaEstado c, Produtor p) {
        if (c.getTalhaoAtual() != null) {
            monitoramento.cancelarAberto(c.getTalhaoAtual());
        }
        c.limparPendencia();
        List<Talhao> lista = talhoes.findByProdutorIdOrderByNome(p.getId());
        if (lista.size() > 1) {
            perguntarTalhao(c, p, "Vamos começar um monitoramento novo. ");
        } else if (lista.size() == 1) {
            c.setTalhaoAtual(lista.get(0));
            envio.texto(p, "Monitoramento novo no talhão " + lista.get(0).getNome()
                    + ". Mande a foto, o áudio ou o texto do primeiro ponto.");
        } else {
            envio.texto(p, "Você ainda não tem talhão cadastrado. Peça ao técnico da cooperativa.");
        }
    }

    private void comandoTrocar(ConversaEstado c, Produtor p) {
        List<Talhao> lista = talhoes.findByProdutorIdOrderByNome(p.getId());
        if (lista.size() <= 1) {
            envio.texto(p, "Você só tem um talhão cadastrado, então não precisa trocar.");
            return;
        }
        perguntarTalhao(c, p, "");
    }

    private void comandoResultado(ConversaEstado c, Produtor p) {
        Talhao talhao = talhaoOuPerguntar(c, p, "");
        if (talhao == null) {
            return;
        }
        Optional<Monitoramento> aberto = monitoramento.monitoramentoAberto(talhao);
        Optional<Monitoramento> concluido = monitoramento.ultimoConcluido(talhao);
        if (aberto.isPresent()) {
            long pontos = monitoramento.pontosDoMonitoramento(aberto.get());
            envio.texto(p, "No talhão " + talhao.getNome() + " você tem " + pontos + " de 3 pontos. "
                    + "Faltam " + (3 - pontos) + " para eu poder decidir.");
        } else if (concluido.isPresent()) {
            Monitoramento m = concluido.get();
            envio.texto(p, "Último resultado do talhão " + talhao.getNome() + " (" + m.getDataHora().toLocalDate() + "):\n"
                    + "Média por pano: " + numero(m.getMediaPercevejos()) + " percevejos, "
                    + numero(m.getMediaLagartas()) + " lagartas, desfolha " + porcento(m.getMediaDesfolha()) + "%.\n"
                    + mensagemDaDecisao(m.getDecisao()));
        } else {
            envio.texto(p, "O talhão " + talhao.getNome() + " ainda não tem monitoramento. Mande o primeiro ponto.");
        }
    }

    private void comandoApliquei(ConversaEstado c, Produtor p) {
        Talhao talhao = talhaoOuPerguntar(c, p, "");
        if (talhao == null) {
            return;
        }
        boolean fazSentido = monitoramento.aplicacaoFazSentido(talhao);
        monitoramento.registrarAplicacaoDoProdutor(talhao);
        if (fazSentido) {
            envio.texto(p, "Anotei a aplicação no talhão " + talhao.getNome() + ". O técnico vai confirmar.");
        } else {
            envio.texto(p, "Pelo monitoramento, este talhão não precisava. Avise o técnico.");
        }
    }

    // ------------------------------------------------------------------ talhão

    /** Devolve o talhão da conversa. Se ainda não há, pergunta ao produtor e devolve null. */
    private Talhao talhaoOuPerguntar(ConversaEstado c, Produtor p, String prefixo) {
        if (c.getTalhaoAtual() != null) {
            return c.getTalhaoAtual();
        }
        List<Talhao> lista = talhoes.findByProdutorIdOrderByNome(p.getId());
        if (lista.isEmpty()) {
            envio.texto(p, "Você ainda não tem talhão cadastrado. Peça ao técnico da cooperativa.");
            return null;
        }
        if (lista.size() == 1) {
            c.setTalhaoAtual(lista.get(0));
            return lista.get(0);
        }
        perguntarTalhao(c, p, prefixo);
        return null;
    }

    private void perguntarTalhao(ConversaEstado c, Produtor p, String prefixo) {
        List<Talhao> lista = talhoes.findByProdutorIdOrderByNome(p.getId());
        c.setTalhaoAtual(null);
        c.limparPendencia();
        c.setEstado(EstadoConversa.ESCOLHENDO_TALHAO);
        String texto = prefixo + "De qual talhão é este pano?";
        if (lista.size() <= 3) {
            envio.botoes(p, texto, lista.stream().map(t -> new Opcao("TALHAO_" + t.getId(), cortar(t.getNome(), 20))).toList());
        } else {
            // WhatsApp aceita no máximo 10 itens na lista; o resto o produtor pode digitar.
            envio.lista(p, texto, "Escolher talhão",
                    lista.stream().limit(10).map(t -> new Opcao("TALHAO_" + t.getId(), cortar(t.getNome(), 24))).toList());
        }
    }

    private void escolherTalhao(ConversaEstado c, Produtor p, Talhao talhao) {
        c.setTalhaoAtual(talhao);
        c.limparPendencia();
        String extra = monitoramento.monitoramentoAberto(talhao)
                .map(m -> " Você já tem " + monitoramento.pontosDoMonitoramento(m) + " ponto(s) neste monitoramento.")
                .orElse("");
        envio.texto(p, "Talhão " + talhao.getNome() + " escolhido." + extra
                + " Mande a foto, o áudio ou o texto do ponto.");
    }

    private Talhao talhaoDoProdutor(Produtor p, String idTexto) {
        try {
            long id = Long.parseLong(idTexto);
            return talhoes.findById(id).filter(t -> t.getProdutor().getId().equals(p.getId())).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Talhao talhaoPorNomeOuNumero(Produtor p, String texto) {
        List<Talhao> lista = talhoes.findByProdutorIdOrderByNome(p.getId());
        String procurado = ParserTexto.comando(texto);
        String numero = texto == null ? "" : texto.trim();
        if (numero.matches("\\d{1,2}") && Integer.parseInt(numero) >= 1 && Integer.parseInt(numero) <= lista.size()) {
            return lista.get(Integer.parseInt(numero) - 1);
        }
        return lista.stream()
                .filter(t -> !procurado.isEmpty() && ParserTexto.comando(t.getNome()).contains(procurado))
                .findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ textos

    private String contar(int percevejos, int lagartas) {
        return percevejos + (percevejos == 1 ? " percevejo" : " percevejos") + " e "
                + lagartas + (lagartas == 1 ? " lagarta" : " lagartas");
    }

    /** "3 percevejos, 4 lagartas e 10% de desfolha" */
    private String contarComDesfolha(int percevejos, int lagartas, int desfolha) {
        return contar(percevejos, lagartas).replace(" e ", ", ") + " e " + desfolha + "% de desfolha";
    }

    /** Desfolha sem casa decimal, igual ao painel e ao cartão de resultado. */
    private String porcento(Double valor) {
        return String.valueOf(Math.round(valor == null ? 0.0 : valor));
    }

    private String numero(Double valor) {
        return String.format(Locale.of("pt", "BR"), "%.1f", valor == null ? 0.0 : valor);
    }

    private String cortar(String texto, int limite) {
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private String mensagemDaDecisao(Decisao decisao) {
        return br.com.panocerto.mip.RegraMip.mensagem(decisao);
    }

    private TipoMensagem tipoParaLog(MensagemEntrada.Tipo tipo) {
        return switch (tipo) {
            case TEXTO, BOTAO -> TipoMensagem.TEXTO;
            case IMAGEM -> TipoMensagem.IMAGEM;
            case AUDIO -> TipoMensagem.AUDIO;
            default -> TipoMensagem.OUTRO;
        };
    }

    private String textoParaLog(MensagemEntrada entrada) {
        return switch (entrada.tipo()) {
            case TEXTO, BOTAO -> entrada.texto();
            case IMAGEM -> "[foto]";
            case AUDIO -> "[áudio]";
            default -> "[mensagem de tipo não suportado]";
        };
    }
}
