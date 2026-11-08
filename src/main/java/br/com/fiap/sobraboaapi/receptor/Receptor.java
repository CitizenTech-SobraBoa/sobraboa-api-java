package br.com.fiap.sobraboaapi.receptor;

import br.com.fiap.sobraboaapi.endereco.Endereco;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import br.com.fiap.sobraboaapi.usuario.Usuario;
import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "receptor")
public class Receptor {

    @Id
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY) @MapsId
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String nomeOrganizacao;
    private String cnpj;
    private String tipo;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    private Integer pessoasAtendidas;
    private boolean possuiRefrigeracao;
    private String horarioFuncionamento;

    @Enumerated(EnumType.STRING) private StatusAprovacao statusAprovacao = StatusAprovacao.PENDENTE;
    private Long aprovadoPor;
    private OffsetDateTime aprovadoEm;
}
