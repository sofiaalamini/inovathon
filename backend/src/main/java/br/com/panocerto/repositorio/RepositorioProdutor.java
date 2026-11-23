package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioProdutor extends JpaRepository<Produtor, Long> {
    List<Produtor> findByCooperativaIdOrderByNome(Long cooperativaId);
    List<Produtor> findByTelefoneIn(Collection<String> telefones);
    boolean existsByTelefone(String telefone);
}
