package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Área da lavoura. O estádio (V1..V19, R1..R8) e a finalidade mudam o nível de ação do MIP. */
@Entity
@Getter
@Setter
public class Talhao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private BigDecimal hectares;
    private String estadio;

    @Enumerated(EnumType.STRING)
    private Finalidade finalidade;

    @ManyToOne
    private Produtor produtor;

    private Double latitude;
    private Double longitude;

    private LocalDateTime criadoEm;

    /** Última vez que o robô lembrou o produtor deste talhão (evita lembrar todo dia). */
    private LocalDateTime ultimoLembreteEm;
}
