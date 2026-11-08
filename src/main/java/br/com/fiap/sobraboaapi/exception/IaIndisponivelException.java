package br.com.fiap.sobraboaapi.exception;

public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String m, Throwable c) {
        super(m, c);
    }
}
