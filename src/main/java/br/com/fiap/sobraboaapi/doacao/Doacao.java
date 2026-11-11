package br.com.fiap.sobraboaapi.doacao;

import br.com.fiap.sobraboaapi.doacaoitem.DoacaoItem;
import br.com.fiap.sobraboaapi.doador.Doador;
import br.com.fiap.sobraboaapi.endereco.Endereco;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "doacao")
public class Doacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doador_id")
    private Doador doador;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "endereco_retirada_id")
    private Endereco enderecoRetirada;

    private String titulo;
    private String descricao;
    @Enumerated(EnumType.STRING) private StatusDoacao status = StatusDoacao.RASCUNHO;
    private OffsetDateTime janelaInicio;
    private OffsetDateTime janelaFim;
    private boolean requerRefrigeracao;
    private String instrucoesRetirada;
    private String fotoUrl;
    private OffsetDateTime criadoEm = OffsetDateTime.now();
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @OneToMany(mappedBy = "doacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DoacaoItem> itens = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        return o instanceof Doacao outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
