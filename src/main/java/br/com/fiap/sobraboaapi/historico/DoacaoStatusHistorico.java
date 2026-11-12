package br.com.fiap.sobraboaapi.historico;

import br.com.fiap.sobraboaapi.doacao.StatusDoacao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "doacao_status_historico")
public class DoacaoStatusHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long doacaoId;

    @Enumerated(EnumType.STRING)
    private StatusDoacao statusAnterior;

    @Enumerated(EnumType.STRING)
    private StatusDoacao statusNovo;

    private Long alteradoPor;

    private OffsetDateTime alteradoEm = OffsetDateTime.now();

    private String observacao;

    @Override
    public boolean equals(Object o) {
        return o instanceof DoacaoStatusHistorico outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
