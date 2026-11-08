package br.com.fiap.sobraboaapi.reserva;

import br.com.fiap.sobraboaapi.doacao.Doacao;
import br.com.fiap.sobraboaapi.receptor.Receptor;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doacao_id")
    private Doacao doacao;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "receptor_id")
    private Receptor receptor;

    @Enumerated(EnumType.STRING) private StatusReserva status = StatusReserva.ATIVA;
    private String codigoRetirada;
    private OffsetDateTime reservadaEm = OffsetDateTime.now();
    private OffsetDateTime expiraEm;
    private OffsetDateTime retiradaEm;
    private OffsetDateTime recebidaEm;
    private String motivoCancelamento;
}
