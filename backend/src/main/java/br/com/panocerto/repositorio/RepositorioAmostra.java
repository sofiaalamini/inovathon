package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioAmostra extends JpaRepository<Amostra, Long> {
    List<Amostra> findByMonitoramentoIdOrderByCriadaEm(Long monitoramentoId);
    long countByMonitoramentoId(Long monitoramentoId);
    List<Amostra> findByMonitoramentoTalhaoProdutorCooperativaIdAndCriadaEmAfter(Long cooperativaId, LocalDateTime depoisDe);}
