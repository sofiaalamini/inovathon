package br.com.panocerto.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Quando o painel vai embutido no backend (Render), endereços como /convite e /testar
 * são telas do React: o servidor entrega o index.html e o React mostra a tela certa.
 * Em desenvolvimento (Vite separado) não há index.html embutido e nada muda.
 */
@Component
public class RotasDoPainel extends OncePerRequestFilter {

    private final boolean painelEmbutido = new ClassPathResource("static/index.html").exists();

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        if (painelEmbutido && ehTelaDoPainel(pedido)) {
            pedido.getRequestDispatcher("/index.html").forward(pedido, resposta);
            return;
        }
        cadeia.doFilter(pedido, resposta);
    }

    /** GET que não é API, mídia, webhook nem arquivo (sem ponto no nome). */
    static boolean ehTelaDoPainel(HttpServletRequest pedido) {
        String caminho = pedido.getRequestURI();
        String ultimo = caminho.substring(caminho.lastIndexOf('/') + 1);
        return "GET".equals(pedido.getMethod()) && !caminho.equals("/")
                && !caminho.startsWith("/api/") && !caminho.startsWith("/midias/")
                && !caminho.startsWith("/webhook/") && !caminho.equals("/error")
                && !ultimo.contains(".");
    }
}
