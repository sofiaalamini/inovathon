package br.com.panocerto.servico;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.config.Relogio;
import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Modo banca: cada avaliador que abre /testar vira um produtor fictício, com dois talhões,
 * e conversa com o mesmo robô. Há limites para um link público não gerar custo sem fim.
 * Só funciona com o simulador (nunca com o WhatsApp real).
 */
@Service
public class ServicoBanca {

    static final int MAXIMO_VISITANTES = 60;
    static final int MAXIMO_MENSAGENS = 80;

    private record Visitante(String telefone, AtomicInteger mensagens) {
    }

    private final Configuracao config;
    private final RepositorioCooperativa cooperativas;
    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioConversaEstado conversas;
    private final RepositorioAmostra amostras;
    private final RepositorioAlerta alertas;
    private final ServicoMonitoramento monitoramento;
    private final ServicoNiveis niveis;
    private final Map<String, Visitante> visitantes = new ConcurrentHashMap<>();
    private final SecureRandom sorteio = new SecureRandom();

    public ServicoBanca(Configuracao config, RepositorioCooperativa cooperativas, RepositorioProdutor produtores,
                        RepositorioTalhao talhoes, RepositorioConversaEstado conversas, RepositorioAmostra amostras,
                        RepositorioAlerta alertas, ServicoMonitoramento monitoramento, ServicoNiveis niveis) {
        this.config = config;
        this.cooperativas = cooperativas;
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.conversas = conversas;
        this.amostras = amostras;
        this.alertas = alertas;
        this.monitoramento = monitoramento;
        this.niveis = niveis;
    }

    public boolean ativo() {
        return config.modoBanca() && !config.whatsappConfigurado();
    }

    /** Cria o produtor fictício e devolve o código que a tela guarda para continuar a conversa. */
    public synchronized String entrar(String nome) {
        exigirAtivo();
        String limpo = nome == null ? "" : nome.strip();
        if (limpo.isEmpty() || limpo.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escreva seu nome (até 40 letras).");
        }
        if (visitantes.size() >= MAXIMO_VISITANTES) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de visitantes atingido.");
        }
        Cooperativa cooperativa = cooperativas.findAll().stream().findFirst().orElseThrow();

        Produtor produtor = new Produtor();
        produtor.setNome(limpo);
        // Começa com 00: nunca é um telefone de verdade
        produtor.setTelefone("00" + (10_000_000_000L + sorteio.nextLong(89_999_999_999L)));
        produtor.setCooperativa(cooperativa);
        produtor.setVisitante(true);
        produtores.save(produtor);
        criarTalhao(produtor, "Sede", "20", "R5", Finalidade.GRAO);
        criarTalhao(produtor, "Várzea", "12", "R3", Finalidade.SEMENTE);

        String codigo = UUID.randomUUID().toString();
        visitantes.put(codigo, new Visitante(produtor.getTelefone(), new AtomicInteger()));
        return codigo;
    }

    public record Ponto(int percevejos, int lagartas, int desfolha) {
    }

    /** limite null: este item não conta neste estádio. */
    /** nota: explica um limite diferente, ex.: "soja para semente". */
    public record Medida(String nome, double media, Double limite, String unidade, String nota) {
    }

    /** economia: estimativa do que se deixa de gastar (ver calcularEconomia). */
    public record Resultado(Decisao decisao, List<Medida> medidas, double hectares, double economia,
                            StatusAlerta alerta, double areaSemPulverizar) {
    }

    public record Resumo(String talhao, List<Ponto> pontos, Resultado resultado) {
    }

    /** O que a tela mostra no topo (ponto 1 de 3) e no cartão de resultado. */
    public Resumo resumo(String telefone) {
        Talhao talhao = conversas.findByTelefone(telefone).map(ConversaEstado::getTalhaoAtual).orElse(null);
        if (talhao == null) {
            return new Resumo(null, List.of(), null);
        }
        Optional<Monitoramento> aberto = monitoramento.monitoramentoAberto(talhao);
        Monitoramento m = aberto.or(() -> monitoramento.ultimoConcluido(talhao)).orElse(null);
        if (m == null) {
            return new Resumo(talhao.getNome(), List.of(), null);
        }
        List<Ponto> pontos = amostras.findByMonitoramentoIdOrderByCriadaEm(m.getId()).stream()
                .map(a -> new Ponto(a.getPercevejos(), a.getLagartas(), a.getDesfolha())).toList();
        if (aberto.isPresent()) {
            return new Resumo(talhao.getNome(), pontos, null);
        }
        RegraMip.Limites limites = niveis.regra().limites(talhao.getEstadio(), talhao.getFinalidade());
        List<Medida> medidas = List.of(
                new Medida("Percevejos", m.getMediaPercevejos(), limites.percevejo(), "por pano",
                        talhao.getFinalidade() == Finalidade.SEMENTE ? "soja para semente" : null),
                new Medida("Lagartas", m.getMediaLagartas(), limites.lagarta(), "por pano", null),
                new Medida("Folha comida", m.getMediaDesfolha(), limites.desfolha(), "%", null));
        StatusAlerta alerta = alertas.findFirstByTalhaoIdOrderByCriadoEmDesc(talhao.getId())
                .filter(a -> a.getMonitoramento() != null && a.getMonitoramento().getId().equals(m.getId()))
                .map(Alerta::getStatus).orElse(null);
        return new Resumo(talhao.getNome(), pontos, new Resultado(m.getDecisao(), medidas,
                talhao.getHectares().doubleValue(), calcularEconomia(talhao, m.getDecisao()), alerta,
                areaSemPulverizar(talhao, m.getDecisao())));
    }

    /** Sem aplicar: a área do talhão. Aplicando: as outras áreas do produtor, que ficam sem pulverizar. */
    private double calcularEconomia(Talhao talhao, Decisao decisao) {
        return areaSemPulverizar(talhao, decisao) * config.custoAplicacaoHa();
    }

    private double areaSemPulverizar(Talhao talhao, Decisao decisao) {
        return decisao != Decisao.ACIMA
                ? talhao.getHectares().doubleValue()
                : talhoes.findByProdutorIdOrderByNome(talhao.getProdutor().getId()).stream()
                        .filter(t -> !t.getId().equals(talhao.getId()))
                        .mapToDouble(t -> t.getHectares().doubleValue()).sum();
    }

    /** Botão "Limpar testes": todos os celulares voltam para a tela de entrada. */
    public void esquecerVisitantes() {
        visitantes.clear();
    }

    public String telefoneDe(String codigo) {
        return visitante(codigo).telefone();
    }

    /** Telefone do visitante, contando uma mensagem no limite dele. */
    public String telefoneParaEnviar(String codigo) {
        Visitante v = visitante(codigo);
        if (v.mensagens().incrementAndGet() > MAXIMO_MENSAGENS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de mensagens deste teste atingido.");
        }
        return v.telefone();
    }

    private Visitante visitante(String codigo) {
        exigirAtivo();
        Visitante v = codigo == null ? null : visitantes.get(codigo);
        if (v == null) {
            throw new ResponseStatusException(HttpStatus.GONE, "Seu teste expirou. Entre de novo.");
        }
        return v;
    }

    private void exigirAtivo() {
        if (!ativo()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "O modo de teste está desligado.");
        }
    }

    private void criarTalhao(Produtor produtor, String nome, String hectares, String estadio, Finalidade finalidade) {
        Talhao t = new Talhao();
        t.setNome(nome);
        t.setProdutor(produtor);
        t.setHectares(new BigDecimal(hectares));
        t.setEstadio(estadio);
        t.setFinalidade(finalidade);
        t.setCriadoEm(Relogio.agora());
        talhoes.save(t);
    }
}
