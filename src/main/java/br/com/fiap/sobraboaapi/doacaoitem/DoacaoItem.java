package br.com.fiap.sobraboaapi.doacaoitem;

import br.com.fiap.sobraboaapi.categoria.CategoriaAlimento;
import br.com.fiap.sobraboaapi.doacao.Doacao;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "doacao_item")
public class DoacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doacao_id")
    private Doacao doacao;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id")
    private CategoriaAlimento categoria;

    private String descricao;
    private BigDecimal quantidade;
    @Enumerated(EnumType.STRING) private UnidadeMedida unidade;
    private BigDecimal pesoEstimadoKg;
    private LocalDate dataValidade;
    private OffsetDateTime preparadoEm;
}
