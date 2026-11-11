package br.com.fiap.sobraboaapi.notificacao;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "notificacao")
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID eventoId;
    private Long usuarioId;
    private String tipo;
    private String mensagem;
    private boolean lida;
    private OffsetDateTime criadaEm = OffsetDateTime.now();

    @Override
    public boolean equals(Object o) {
        return o instanceof Notificacao outra && getId() != null && getId().equals(outra.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}