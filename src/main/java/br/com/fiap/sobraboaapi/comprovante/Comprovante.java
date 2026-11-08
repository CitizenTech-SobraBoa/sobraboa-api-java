package br.com.fiap.sobraboaapi.comprovante;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Data
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
}
