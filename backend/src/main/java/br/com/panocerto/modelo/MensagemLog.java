package br.com.panocerto.modelo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Histórico de tudo que entrou e saiu pelo WhatsApp (ou pelo simulador). */
@Entity
@Getter
@Setter
public class MensagemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vazio quando o número ainda não está cadastrado. */
    @ManyToOne
    private Produtor produtor;

    private String telefone;

    @Enumerated(EnumType.STRING)
    private Direcao direcao;

    @Enumerated(EnumType.STRING)
    private TipoMensagem tipo;

    private String texto;

    /** Botões ou itens de lista, um por linha no formato "id|título". */
    private String opcoes;

    private String urlMidia;

    /** Id da mensagem na Meta (wamid). Serve para ignorar mensagens repetidas. */
    private String idExterno;

    private LocalDateTime criadaEm;
}
