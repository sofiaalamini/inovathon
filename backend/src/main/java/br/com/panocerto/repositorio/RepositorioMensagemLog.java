package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioMensagemLog extends JpaRepository<MensagemLog, Long> {
    List<MensagemLog> findByTelefoneOrderByCriadaEmAsc(String telefone);
    Optional<MensagemLog> findFirstByTelefoneAndDirecaoOrderByCriadaEmDesc(String telefone, Direcao direcao);
    Optional<MensagemLog> findFirstByTelefoneOrderByCriadaEmDesc(String telefone);
    boolean existsByIdExterno(String idExterno);
    void deleteByTelefone(String telefone);}
