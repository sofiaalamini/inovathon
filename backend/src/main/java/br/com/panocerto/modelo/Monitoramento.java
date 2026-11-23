package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Um monitoramento = vários pontos (amostras) de um talhão, que terminam numa decisão. */
@Entity
@Getter
@Setter
public class Monitoramento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Talhao talhao;

    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    private StatusMonitoramento status;

    @Enumerated(EnumType.STRING)
    private Decisao decisao;

    private Double mediaPercevejos;
    private Double mediaLagartas;
    private Double mediaDesfolha;
}
