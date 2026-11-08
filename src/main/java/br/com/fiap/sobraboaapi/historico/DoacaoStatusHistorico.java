package br.com.fiap.sobraboaapi.historico;

import br.com.fiap.sobraboaapi.doacao.StatusDoacao;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Data
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
}
