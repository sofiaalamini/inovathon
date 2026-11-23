package br.com.panocerto.ia;

/** A IA pedida não está disponível (sem chave). A mensagem já é amigável para o produtor. */
public class IaIndisponivelException extends RuntimeException {

    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
