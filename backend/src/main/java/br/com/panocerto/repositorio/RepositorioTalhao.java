package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioTalhao extends JpaRepository<Talhao, Long> {
    List<Talhao> findByProdutorIdOrderByNome(Long produtorId);
    List<Talhao> findByProdutorCooperativaIdOrderByNome(Long cooperativaId);
}
