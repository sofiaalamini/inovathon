package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Aplicação de inseticida num talhão. "Confirmada" quando o técnico confere. */
@Entity
@Getter
@Setter
public class Aplicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Talhao talhao;

    private LocalDateTime dataHora;
    private boolean confirmada;
}
