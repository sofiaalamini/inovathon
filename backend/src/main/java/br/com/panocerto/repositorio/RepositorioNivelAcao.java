package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioNivelAcao extends JpaRepository<NivelAcao, Long> {
    Optional<NivelAcao> findByChave(String chave);
    List<NivelAcao> findAllByOrderById();
}
