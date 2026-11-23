package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Cooperativa que usa o painel. Produtores e técnicos pertencem a uma cooperativa. */
@Entity
@Getter
@Setter
public class Cooperativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
}
