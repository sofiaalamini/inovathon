package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioConversaEstado extends JpaRepository<ConversaEstado, Long> {
    Optional<ConversaEstado> findByTelefone(String telefone);
    List<ConversaEstado> findByTalhaoAtualId(Long talhaoId);
    void deleteByTelefone(String telefone);
}
