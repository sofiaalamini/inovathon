package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Um nível de ação do MIP (ex.: LAGARTA = 20 por pano). Editável na tela "Regras". */
@Entity
@Getter
@Setter
public class NivelAcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chave;
    private String descricao;
    private double valor;
    private String unidade;
}
