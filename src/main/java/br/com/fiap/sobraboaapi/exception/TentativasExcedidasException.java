package br.com.fiap.sobraboaapi.exception;

public class TentativasExcedidasException extends RuntimeException {
    public TentativasExcedidasException() {
        super("Muitas tentativas de login. Tente novamente em 15 minutos.");
    }
}