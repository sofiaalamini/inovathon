package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Produtor de soja. O telefone (só dígitos, com DDI) é como o robô o reconhece. */
@Entity
@Getter
@Setter
public class Produtor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String telefone;

    @ManyToOne
    private Cooperativa cooperativa;
}
