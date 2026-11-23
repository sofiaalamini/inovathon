package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioAplicacao extends JpaRepository<Aplicacao, Long> {
    List<Aplicacao> findByTalhaoIdOrderByDataHoraDesc(Long talhaoId);
    boolean existsByTalhaoId(Long talhaoId);
    List<Aplicacao> findByTalhaoProdutorCooperativaIdAndDataHoraAfter(Long cooperativaId, LocalDateTime depoisDe);
    Optional<Aplicacao> findFirstByTalhaoIdAndConfirmadaFalseAndDataHoraAfterOrderByDataHoraDesc(Long talhaoId, LocalDateTime depoisDe);}
