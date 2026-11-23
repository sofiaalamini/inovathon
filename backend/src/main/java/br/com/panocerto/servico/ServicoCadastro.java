package br.com.panocerto.servico;

import br.com.panocerto.config.Relogio;
import br.com.panocerto.mip.Estadio;
import br.com.panocerto.modelo.*;
import br.com.panocerto.repositorio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

/** Regras de cadastro de produtores e talhões. */
@Service
public class ServicoCadastro {

    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioMonitoramento monitoramentos;
    private final RepositorioAplicacao aplicacoes;
    private final RepositorioAlerta alertas;
    private final RepositorioConversaEstado conversas;
    private final RepositorioMensagemLog mensagens;
    private final Acesso acesso;

    public ServicoCadastro(RepositorioProdutor produtores, RepositorioTalhao talhoes,
                           RepositorioMonitoramento monitoramentos, RepositorioAplicacao aplicacoes,
                           RepositorioAlerta alertas, RepositorioConversaEstado conversas,
                           RepositorioMensagemLog mensagens, Acesso acesso) {
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.monitoramentos = monitoramentos;
        this.aplicacoes = aplicacoes;
        this.alertas = alertas;
        this.conversas = conversas;
        this.mensagens = mensagens;
        this.acesso = acesso;
    }

    // ---------------------------------------------------------------- produtor

    public Produtor criarProdutor(Cooperativa cooperativa, String nome, String telefone) {
        Produtor produtor = new Produtor();
        produtor.setCooperativa(cooperativa);
        preencherProdutor(produtor, nome, telefone);
        return produtores.save(produtor);
    }

    public Produtor atualizarProdutor(Produtor produtor, String nome, String telefone) {
        preencherProdutor(produtor, nome, telefone);
        return produtores.save(produtor);
    }

    private void preencherProdutor(Produtor produtor, String nome, String telefone) {
        String digitos = Telefone.normalizar(telefone);
        if (!Telefone.valido(digitos)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Telefone no formato internacional, com DDI e DDD. Exemplo: +55 45 99999-0001");
        }
        // Já existe outro produtor com este número (com ou sem o 9 depois do DDD)?
        boolean repetido = produtores.findByTelefoneIn(Telefone.variantes(digitos)).stream()
                .anyMatch(outro -> !outro.getId().equals(produtor.getId()));
        if (repetido) {
            throw new ResponseStatusException(CONFLICT, "Já existe um produtor com este telefone.");
        }
        produtor.setNome(nome.trim());
        produtor.setTelefone(digitos);
    }

    /** Apaga o produtor e a conversa dele (dados pessoais). Só se não houver talhões. */
    @Transactional
    public void excluirProdutor(Produtor produtor) {
        if (!talhoes.findByProdutorIdOrderByNome(produtor.getId()).isEmpty()) {
            throw new ResponseStatusException(CONFLICT, "Este produtor ainda tem talhões. Remova os talhões primeiro.");
        }
        conversas.deleteByTelefone(produtor.getTelefone());
        mensagens.deleteByTelefone(produtor.getTelefone());
        produtores.delete(produtor);
    }

    // ---------------------------------------------------------------- talhão

    public Talhao criarTalhao(Produtor produtor, String nome, BigDecimal hectares, String estadio,
                              Finalidade finalidade, Double latitude, Double longitude) {
        Talhao talhao = new Talhao();
        talhao.setProdutor(produtor);
        talhao.setCriadoEm(Relogio.agora());
        preencherTalhao(talhao, nome, hectares, estadio, finalidade, latitude, longitude);
        return talhoes.save(talhao);
    }

    public Talhao atualizarTalhao(Talhao talhao, Produtor produtor, String nome, BigDecimal hectares, String estadio,
                                  Finalidade finalidade, Double latitude, Double longitude) {
        talhao.setProdutor(produtor);
        preencherTalhao(talhao, nome, hectares, estadio, finalidade, latitude, longitude);
        return talhoes.save(talhao);
    }

    private void preencherTalhao(Talhao talhao, String nome, BigDecimal hectares, String estadio,
                                 Finalidade finalidade, Double latitude, Double longitude) {
        if (!Estadio.valido(estadio)) {
            throw new ResponseStatusException(BAD_REQUEST, "Estádio inválido. Use V1 a V19 ou R1 a R8.");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe latitude e longitude juntas, ou nenhuma das duas.");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(BAD_REQUEST, "Latitude ou longitude fora do intervalo válido.");
        }
        talhao.setNome(nome.trim());
        talhao.setHectares(hectares);
        talhao.setEstadio(estadio);
        talhao.setFinalidade(finalidade);
        talhao.setLatitude(latitude);
        talhao.setLongitude(longitude);
    }

    /** Talhão com histórico não pode ser apagado: o histórico é a prova das decisões. */
    @Transactional
    public void excluirTalhao(Talhao talhao) {
        if (monitoramentos.existsByTalhaoId(talhao.getId()) || aplicacoes.existsByTalhaoId(talhao.getId())
                || alertas.existsByTalhaoId(talhao.getId())) {
            throw new ResponseStatusException(CONFLICT, "Este talhão já tem histórico e não pode ser excluído.");
        }
        conversas.findByTalhaoAtualId(talhao.getId()).forEach(c -> {
            c.setTalhaoAtual(null);
            c.limparPendencia();
            conversas.save(c);
        });
        talhoes.delete(talhao);
    }
}
