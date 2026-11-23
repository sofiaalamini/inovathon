package br.com.panocerto.servico;

import br.com.panocerto.mip.RegraMip;
import br.com.panocerto.modelo.NivelAcao;
import br.com.panocerto.repositorio.RepositorioNivelAcao;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Lê e edita os níveis de ação que ficam no banco e monta a RegraMip com eles. */
@Service
public class ServicoNiveis {

    private final RepositorioNivelAcao repositorio;

    public ServicoNiveis(RepositorioNivelAcao repositorio) {
        this.repositorio = repositorio;
    }

    public List<NivelAcao> listar() {
        return repositorio.findAllByOrderById();
    }

    public NivelAcao atualizar(Long id, double valor) {
        NivelAcao nivel = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Nível não encontrado."));
        if (valor <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "O valor precisa ser maior que zero.");
        }
        if (nivel.getChave().equals("PERTO_PERCENTUAL") && valor > 100) {
            throw new ResponseStatusException(BAD_REQUEST, "O percentual de \"perto\" vai de 1 a 100.");
        }
        nivel.setValor(valor);
        return repositorio.save(nivel);
    }

    /** Monta a regra com os valores atuais da tabela (assim a edição no painel vale na hora). */
    public RegraMip regra() {
        Map<String, Double> valores = new HashMap<>();
        listar().forEach(n -> valores.put(n.getChave(), n.getValor()));
        RegraMip.Niveis padrao = RegraMip.Niveis.padrao();
        return new RegraMip(new RegraMip.Niveis(
                valores.getOrDefault("PERCEVEJO_GRAO", padrao.percevejoGrao()),
                valores.getOrDefault("PERCEVEJO_SEMENTE", padrao.percevejoSemente()),
                valores.getOrDefault("LAGARTA", padrao.lagarta()),
                valores.getOrDefault("DESFOLHA_VEGETATIVA", padrao.desfolhaVegetativa()),
                valores.getOrDefault("DESFOLHA_REPRODUTIVA", padrao.desfolhaReprodutiva()),
                valores.getOrDefault("PERTO_PERCENTUAL", padrao.perto())));
    }
}
