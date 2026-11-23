package br.com.panocerto.seguranca;

import br.com.panocerto.config.Configuracao;
import br.com.panocerto.modelo.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/** Cria e confere os tokens JWT do painel. */
@Service
public class ServicoJwt {

    private final SecretKey chave;
    private final int horas;

    public ServicoJwt(Configuracao config) {
        this.chave = Keys.hmacShaKeyFor(config.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.horas = config.jwtHoras();
    }

    public String gerar(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("nome", usuario.getNome())
                .claim("cooperativaId", usuario.getCooperativa().getId())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(horas, ChronoUnit.HOURS)))
                .signWith(chave)
                .compact();
    }

    /** Devolve o usuário do token, ou vazio se o token for inválido ou estiver vencido. */
    public Optional<UsuarioLogado> ler(String token) {
        try {
            Claims dados = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioLogado(
                    Long.valueOf(dados.getSubject()),
                    dados.get("email", String.class),
                    dados.get("nome", String.class),
                    dados.get("cooperativaId", Long.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
