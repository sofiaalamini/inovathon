package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Um ponto de pano de batida: contagem de percevejos e lagartas e % de desfolha. */
@Entity
@Getter
@Setter
public class Amostra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Monitoramento monitoramento;

    private int percevejos;
    private int lagartas;
    private int desfolha;

    @Enumerated(EnumType.STRING)
    private Origem origem;

    /** Caminho da foto ou do áudio guardado (ex.: /midias/abc.jpg). */
    private String urlMidia;

    /** Texto que o Whisper entendeu, quando a origem é áudio. */
    private String transcricao;

    private LocalDateTime criadaEm;
}
