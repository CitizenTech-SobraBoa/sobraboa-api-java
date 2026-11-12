package br.com.fiap.sobraboaapi.comprovante;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "comprovante")
public class Comprovante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long reservaId;
    private String codigoVerificacao;
    private BigDecimal pesoTotalKg;
    private OffsetDateTime emitidoEm = OffsetDateTime.now();
    private String urlPdf;

    @Override
    public boolean equals(Object o) {
        return o instanceof Comprovante outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
