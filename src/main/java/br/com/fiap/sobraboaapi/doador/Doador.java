package br.com.fiap.sobraboaapi.doador;

import br.com.fiap.sobraboaapi.endereco.Endereco;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import br.com.fiap.sobraboaapi.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "doador")
public class Doador {

    @Id
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY) @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String tipoEstabelecimento;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Enumerated(EnumType.STRING) private StatusAprovacao statusAprovacao = StatusAprovacao.PENDENTE;
    private Long aprovadoPor;
    private OffsetDateTime aprovadoEm;

    @Override
    public boolean equals(Object o) {
        return o instanceof Doador outro && getUsuarioId() != null && getUsuarioId().equals(outro.getUsuarioId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}