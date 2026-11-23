package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioAlerta extends JpaRepository<Alerta, Long> {
    List<Alerta> findByTalhaoProdutorCooperativaIdOrderByCriadoEmDesc(Long cooperativaId);
    Optional<Alerta> findFirstByTalhaoIdAndStatusOrderByCriadoEmDesc(Long talhaoId, StatusAlerta status);
    Optional<Alerta> findFirstByTalhaoIdOrderByCriadoEmDesc(Long talhaoId);
    boolean existsByTalhaoId(Long talhaoId);}
