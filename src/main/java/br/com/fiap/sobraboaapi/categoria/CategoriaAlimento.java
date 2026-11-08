package br.com.fiap.sobraboaapi.categoria;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "categoria_alimento")
public class CategoriaAlimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;
    private String nome;
    private boolean requerRefrigeracaoPadrao;
    private String observacaoCuidado;
}
