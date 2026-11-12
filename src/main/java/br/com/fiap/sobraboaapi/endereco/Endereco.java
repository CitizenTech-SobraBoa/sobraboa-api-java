package br.com.fiap.sobraboaapi.endereco;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "endereco")
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String uf;
    private String cep;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Override
    public boolean equals(Object o) {
        return o instanceof Endereco outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
