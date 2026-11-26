package br.com.panocerto.controle;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.config.SemeadorDados;
import br.com.panocerto.servico.Acesso;
import br.com.panocerto.servico.ServicoBanca;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Botão "Limpar testes" do painel. Só existe no modo banca (dia do evento): apaga tudo e recria o exemplo. */
@RestController
public class TestesControle {

    private final Configuracao config;
    private final SemeadorDados semeador;
    private final ServicoBanca banca;
    private final Acesso acesso;

    public TestesControle(Configuracao config, SemeadorDados semeador, ServicoBanca banca, Acesso acesso) {
        this.config = config;
        this.semeador = semeador;
        this.banca = banca;
        this.acesso = acesso;
    }

    @PostMapping("/api/sistema/limpar-testes")
    public Map<String, Object> limpar() {
        if (!config.modoBanca()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Só funciona no modo banca.");
        }
        semeador.recriarDadosDeExemplo(acesso.cooperativaId());
        banca.esquecerVisitantes();
        return Map.of("ok", true);
    }
}
