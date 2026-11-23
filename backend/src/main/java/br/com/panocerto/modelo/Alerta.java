package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aviso para o técnico: um talhão ficou ACIMA do nível e alguém precisa decidir. */
@Entity
@Getter
@Setter
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Talhao talhao;

    @ManyToOne
    private Monitoramento monitoramento;

    private LocalDateTime criadoEm;

    @Enumerated(EnumType.STRING)
    private StatusAlerta status;

    private LocalDateTime resolvidoEm;
}
