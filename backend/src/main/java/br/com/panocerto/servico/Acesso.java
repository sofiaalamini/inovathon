package br.com.panocerto.servico;

import br.com.panocerto.modelo.Alerta;
import br.com.panocerto.modelo.Produtor;
import br.com.panocerto.modelo.Talhao;
import br.com.panocerto.repositorio.RepositorioAlerta;
import br.com.panocerto.repositorio.RepositorioProdutor;
import br.com.panocerto.repositorio.RepositorioTalhao;
import br.com.panocerto.seguranca.UsuarioLogado;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

/**
 * Garante que o técnico só enxerga dados da própria cooperativa.
 * Se o registro é de outra cooperativa, responde "não encontrado" (não revela que existe).
 */
@Component
public class Acesso {

    private final RepositorioProdutor produtores;
    private final RepositorioTalhao talhoes;
    private final RepositorioAlerta alertas;

    public Acesso(RepositorioProdutor produtores, RepositorioTalhao talhoes, RepositorioAlerta alertas) {
        this.produtores = produtores;
        this.talhoes = talhoes;
        this.alertas = alertas;
    }

    public UsuarioLogado usuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioLogado usuario)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Entre no sistema para continuar.");
        }
        return usuario;
    }

    public Long cooperativaId() {
        return usuario().cooperativaId();
    }

    public Produtor produtor(Long id) {
        return produtores.findById(id)
                .filter(p -> p.getCooperativa().getId().equals(cooperativaId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Produtor não encontrado."));
    }

    public Talhao talhao(Long id) {
        return talhoes.findById(id)
                .filter(t -> t.getProdutor().getCooperativa().getId().equals(cooperativaId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Talhão não encontrado."));
    }

    public Alerta alerta(Long id) {
        return alertas.findById(id)
                .filter(a -> a.getTalhao().getProdutor().getCooperativa().getId().equals(cooperativaId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Alerta não encontrado."));
    }
}
