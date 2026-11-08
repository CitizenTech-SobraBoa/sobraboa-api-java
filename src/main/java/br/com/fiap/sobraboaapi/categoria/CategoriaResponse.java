package br.com.fiap.sobraboaapi.categoria;

public record CategoriaResponse(Short id, String nome, boolean requerRefrigeracaoPadrao, String observacaoCuidado) {
    public static CategoriaResponse de(CategoriaAlimento c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.isRequerRefrigeracaoPadrao(), c.getObservacaoCuidado());
    }
}