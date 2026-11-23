package br.com.panocerto.seguranca;

/** Quem está logado no painel (guardado dentro do token JWT). */
public record UsuarioLogado(Long id, String email, String nome, Long cooperativaId) {
}
