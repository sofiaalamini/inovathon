package br.com.panocerto.controle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Todo erro da API sai no mesmo formato: {"mensagem": "..."} e sem detalhes internos. */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorErros.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensagem", e.getReason() == null ? "Erro." : e.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getDefaultMessage()).findFirst().orElse("Dados inválidos.");
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> arquivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("mensagem", "Arquivo grande demais (máximo 20 MB)."));
    }

    /** Arquivo do painel que não existe: 404, sem poluir o log. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> naoAchado(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensagem", "Não encontrado."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensagem", "Algo deu errado. Tente de novo."));
    }
}
