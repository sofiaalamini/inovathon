package br.com.panocerto.repositorio;

import br.com.panocerto.modelo.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.*;

public interface RepositorioMonitoramento extends JpaRepository<Monitoramento, Long> {
    Optional<Monitoramento> findFirstByTalhaoIdAndStatusOrderByDataHoraDesc(Long talhaoId, StatusMonitoramento status);
    Optional<Monitoramento> findFirstByTalhaoIdAndStatusNotOrderByDataHoraDesc(Long talhaoId, StatusMonitoramento status);
    boolean existsByTalhaoId(Long talhaoId);
    List<Monitoramento> findByTalhaoIdOrderByDataHoraDesc(Long talhaoId);
    List<Monitoramento> findByTalhaoProdutorCooperativaIdAndStatusAndDataHoraBetweenOrderByDataHora(
            Long cooperativaId, StatusMonitoramento status, LocalDateTime de, LocalDateTime ate);}
