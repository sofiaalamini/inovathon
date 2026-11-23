package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Memória da conversa com um telefone: talhão escolhido e amostra esperando a desfolha. */
@Entity
@Getter
@Setter
public class ConversaEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String telefone;

    @ManyToOne
    private Produtor produtor;

    @ManyToOne
    private Talhao talhaoAtual;

    @Enumerated(EnumType.STRING)
    private EstadoConversa estado = EstadoConversa.LIVRE;

    // Amostra pendente (ainda falta a desfolha ou a confirmação da contagem)
    private Integer pendentePercevejos;
    private Integer pendenteLagartas;

    @Enumerated(EnumType.STRING)
    private Origem pendenteOrigem;

    private String pendenteUrlMidia;
    private String pendenteTranscricao;

    private LocalDateTime atualizadaEm;

    /** Limpa a amostra pendente e volta ao estado livre. */
    public void limparPendencia() {
        estado = EstadoConversa.LIVRE;
        pendentePercevejos = null;
        pendenteLagartas = null;
        pendenteOrigem = null;
        pendenteUrlMidia = null;
        pendenteTranscricao = null;
    }
}
