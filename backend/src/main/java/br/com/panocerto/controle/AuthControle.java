package br.com.panocerto.controle;

import br.com.panocerto.modelo.Usuario;
import br.com.panocerto.repositorio.RepositorioUsuario;
import br.com.panocerto.seguranca.ServicoJwt;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/auth")
public class AuthControle {

    public record LoginPedido(@NotBlank(message = "Informe o e-mail.") String email,
                              @NotBlank(message = "Informe a senha.") String senha) {
    }

    public record LoginResposta(String token, String nome) {
    }

    private final RepositorioUsuario usuarios;
    private final PasswordEncoder encoder;
    private final ServicoJwt jwt;

    public AuthControle(RepositorioUsuario usuarios, PasswordEncoder encoder, ServicoJwt jwt) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResposta entrar(@Valid @RequestBody LoginPedido pedido) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(pedido.email().trim())
                .filter(u -> encoder.matches(pedido.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "E-mail ou senha incorretos."));
        return new LoginResposta(jwt.gerar(usuario), usuario.getNome());
    }
}
