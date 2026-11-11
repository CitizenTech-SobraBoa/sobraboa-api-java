package br.com.fiap.sobraboaapi.ia;

public interface SugestaoGateway {
    SugestaoDoacao sugerir(String texto, String hoje, String categorias);
}

