package br.com.panocerto.config;

import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.mip.RegraMip.Ponto;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.*;
import br.com.panocerto.servico.ServicoNiveis;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dados de exemplo (fictícios) para o painel já abrir com informação.
 * Só roda se o banco estiver vazio. Tudo é calculado a partir de hoje.
 */
@Component
public class SemeadorDados implements CommandLineRunner {

    // nome, índice do produtor, hectares, estádio, finalidade, dias desde o último pano,
    // percevejos, lagartas e desfolha médios da última semana
    private record Modelo(String nome, int produtor, String hectares, String estadio, Finalidade finalidade,
                          int diasUltimo, double percevejos, double lagartas, double desfolha) {
    }

    private static final List<Modelo> TALHOES = List.of(
            new Modelo("Sede", 0, "42.5", "R5", Finalidade.GRAO, 2, 3.0, 5, 6),          // ACIMA
            new Modelo("Várzea", 0, "28", "R3", Finalidade.SEMENTE, 3, 0.8, 3, 4),       // ACIMA (semente)
            new Modelo("Beira do Rio", 0, "19.8", "V8", Finalidade.GRAO, 9, 0, 8, 12),   // sem monitorar
            new Modelo("Lote 1", 1, "35", "R4", Finalidade.GRAO, 1, 1.5, 4, 5),          // PERTO
            new Modelo("Lote 2", 1, "22", "R2", Finalidade.GRAO, 4, 5, 3, 5),            // percevejo ignorado
            new Modelo("Lote 3", 1, "30", "V4", Finalidade.GRAO, 8, 0, 16, 8),           // PERTO, sem monitorar
            new Modelo("Lote 4", 1, "18.5", "R6", Finalidade.SEMENTE, 1, 0.3, 2, 3),     // ABAIXO
            new Modelo("Chácara", 2, "15", "R1", Finalidade.GRAO, 5, 0, 4, 12),          // PERTO (desfolha)
            new Modelo("Sítio Boa Vista", 3, "33", "V10", Finalidade.GRAO, 12, 0, 5, 9), // sem monitorar
            new Modelo("Fazenda Nova", 4, "60", "R5", Finalidade.GRAO, 3, 1.0, 2, 4));   // ABAIXO

    private static final String[][] PRODUTORES = {
            {"Valdir", "5545999990001"},
            {"Marlene", "5545999990002"},
            {"Joaquim", "5545999990003"},
            {"Sandra", "5545999990004"},
            {"Ivo", "5545999990005"}};

    private final Configuracao config;
    private final RepositorioCooperativa cooperativas;
    private final RepositorioUsuario usuarios;
    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioMonitoramento monitoramentos;
    private final RepositorioAmostra amostras;
    private final RepositorioAplicacao aplicacoes;
    private final RepositorioAlerta alertas;
    private final RepositorioMensagemLog mensagens;
    private final ServicoNiveis niveis;
    private final PasswordEncoder encoder;

    public SemeadorDados(Configuracao config, RepositorioCooperativa cooperativas, RepositorioUsuario usuarios,
                         RepositorioProdutor produtores, RepositorioTalhao talhoes,
                         RepositorioMonitoramento monitoramentos, RepositorioAmostra amostras,
                         RepositorioAplicacao aplicacoes, RepositorioAlerta alertas,
                         RepositorioMensagemLog mensagens, ServicoNiveis niveis, PasswordEncoder encoder) {
        this.config = config;
        this.cooperativas = cooperativas;
        this.usuarios = usuarios;
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.monitoramentos = monitoramentos;
        this.amostras = amostras;
        this.aplicacoes = aplicacoes;
        this.alertas = alertas;
        this.mensagens = mensagens;
        this.niveis = niveis;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!config.carregarDadosExemplo() || cooperativas.count() > 0) {
            return;
        }

        Cooperativa coop = new Cooperativa();
        coop.setNome("Cooperativa Exemplo do Oeste");
        cooperativas.save(coop);

        Usuario tecnico = new Usuario();
        tecnico.setNome("Técnico Exemplo");
        tecnico.setEmail("tecnico@panocerto.com");
        tecnico.setSenhaHash(encoder.encode("123456"));
        tecnico.setPapel(Papel.TECNICO);
        tecnico.setCooperativa(coop);
        usuarios.save(tecnico);

        List<Produtor> listaProdutores = new ArrayList<>();
        for (String[] dados : PRODUTORES) {
            Produtor p = new Produtor();
            p.setNome(dados[0]);
            p.setTelefone(dados[1]);
            p.setCooperativa(coop);
            listaProdutores.add(produtores.save(p));
        }

        RegraMip regra = niveis.regra();
        LocalDateTime agora = Relogio.agora();
        List<Talhao> criados = new ArrayList<>();
        for (Modelo m : TALHOES) {
            Talhao t = new Talhao();
            t.setNome(m.nome());
            t.setProdutor(listaProdutores.get(m.produtor()));
            t.setHectares(new BigDecimal(m.hectares()));
            t.setEstadio(m.estadio());
            t.setFinalidade(m.finalidade());
            t.setCriadoEm(agora.minusDays(70));
            talhoes.save(t);
            criados.add(t);
            criarHistorico(t, m, regra, agora);
        }

        // Algumas aplicações já feitas na safra
        criarAplicacao(criados.get(0), agora.minusDays(30), true);
        criarAplicacao(criados.get(3), agora.minusDays(25), true);
        criarAplicacao(criados.get(3), agora.minusDays(10), true);
        criarAplicacao(criados.get(9), agora.minusDays(20), true);

        criarConversaExemplo(listaProdutores.get(0), agora);
    }

    /** Um monitoramento por semana, com 3 pontos, e a pressão crescendo até a última semana. */
    private void criarHistorico(Talhao t, Modelo m, RegraMip regra, LocalDateTime agora) {
        double[] variacao = {0.7, 1.0, 1.3};
        Origem[] origens = {Origem.FOTO, Origem.AUDIO, Origem.TEXTO};
        for (int semana = 5; semana >= 0; semana--) {
            double fator = Math.max(0, 1 - 0.14 * semana);   // semanas antigas tinham menos praga
            LocalDateTime quando = LocalDateTime.of(LocalDate.from(agora).minusDays(m.diasUltimo() + 7L * semana),
                    LocalTime.of(9, 0));

            Monitoramento mon = new Monitoramento();
            mon.setTalhao(t);
            mon.setDataHora(quando);
            mon.setStatus(StatusMonitoramento.CONCLUIDO);
            monitoramentos.save(mon);

            List<Ponto> pontos = new ArrayList<>();
            for (int k = 0; k < 3; k++) {
                int percevejos = (int) Math.round(m.percevejos() * fator * variacao[k]);
                int lagartas = (int) Math.round(m.lagartas() * fator * variacao[k]);
                int desfolha = (int) Math.round(m.desfolha() * fator * variacao[k]);
                pontos.add(new Ponto(percevejos, lagartas, desfolha));

                Amostra a = new Amostra();
                a.setMonitoramento(mon);
                a.setPercevejos(percevejos);
                a.setLagartas(lagartas);
                a.setDesfolha(desfolha);
                a.setOrigem(origens[k]);
                if (origens[k] == Origem.AUDIO) {
                    a.setTranscricao("bati três vez, deu " + percevejos + " percevejo e " + lagartas + " lagarta");
                }
                a.setCriadaEm(quando.plusMinutes(10L * k));
                amostras.save(a);
            }

            RegraMip.Resultado r = regra.decidir(pontos, t.getEstadio(), t.getFinalidade());
            mon.setDecisao(r.decisao());
            mon.setMediaPercevejos(r.mediaPercevejos());
            mon.setMediaLagartas(r.mediaLagartas());
            mon.setMediaDesfolha(r.mediaDesfolha());
            monitoramentos.save(mon);

            // Só o monitoramento mais recente gera alerta pendente
            if (semana == 0 && r.decisao() == Decisao.ACIMA) {
                Alerta alerta = new Alerta();
                alerta.setTalhao(t);
                alerta.setMonitoramento(mon);
                alerta.setCriadoEm(quando.plusMinutes(30));
                alerta.setStatus(StatusAlerta.PENDENTE);
                alertas.save(alerta);
            }
        }
    }

    private void criarAplicacao(Talhao t, LocalDateTime quando, boolean confirmada) {
        Aplicacao a = new Aplicacao();
        a.setTalhao(t);
        a.setDataHora(quando);
        a.setConfirmada(confirmada);
        aplicacoes.save(a);
    }

    private void criarConversaExemplo(Produtor valdir, LocalDateTime agora) {
        String[][] falas = {
                {"ENTRADA", "2 percevejos 3 lagartas 10%"},
                {"SAIDA", "Ponto 1 registrado: 2 percevejos, 3 lagartas, 10% de desfolha. Faltam 2 pontos. Bata o pano em outro lugar do talhão."},
                {"ENTRADA", "ajuda"}};
        int minutos = 90;
        for (String[] fala : falas) {
            MensagemLog msg = new MensagemLog();
            msg.setProdutor(valdir);
            msg.setTelefone(valdir.getTelefone());
            msg.setDirecao(Direcao.valueOf(fala[0]));
            msg.setTipo(TipoMensagem.TEXTO);
            msg.setTexto(fala[1]);
            msg.setCriadaEm(agora.minusMinutes(minutos));
            mensagens.save(msg);
            minutos -= 2;
        }
    }
}
