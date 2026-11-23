package br.com.panocerto.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Lê o cabeçalho "Authorization: Bearer ..." e, se o token for bom, marca a requisição como autenticada. */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final ServicoJwt jwt;

    public FiltroJwt(ServicoJwt jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");
        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            jwt.ler(cabecalho.substring(7)).ifPresent(usuario -> {
                var autenticacao = new UsernamePasswordAuthenticationToken(
                        usuario, null, List.of(new SimpleGrantedAuthority("ROLE_TECNICO")));
                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            });
        }
        chain.doFilter(request, response);
    }
}
