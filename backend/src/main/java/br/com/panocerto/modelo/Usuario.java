package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Técnico ou administrador que entra no painel com e-mail e senha. */
@Entity
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    private Papel papel;

    @ManyToOne
    private Cooperativa cooperativa;
}
