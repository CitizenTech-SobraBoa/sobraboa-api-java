package br.com.fiap.sobraboaapi.categoria;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "categoria_alimento")
public class CategoriaAlimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;
    private String nome;
    private boolean requerRefrigeracaoPadrao;
    private String observacaoCuidado;

    @Override
    public boolean equals(Object o) {
        return o instanceof CategoriaAlimento outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
