package br.com.panocerto.controle;

import br.com.panocerto.servico.ServicoArmazenamento;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/** Entrega as fotos e áudios guardados (usado pelas tags <img> e <audio> do painel). */
@RestController
public class MidiaControle {

    private final ServicoArmazenamento armazenamento;

    public MidiaControle(ServicoArmazenamento armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/midias/{nome:.+}")
    public ResponseEntity<byte[]> baixar(@PathVariable String nome) {
        return armazenamento.ler(nome)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(ServicoArmazenamento.tipoPelaExtensao(nome)))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }
}
