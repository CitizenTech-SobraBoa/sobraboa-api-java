package br.com.fiap.sobraboaapi.usuario;

import jakarta.persistence.*;
import lombok.Data;

import java.time.OffsetDateTime;

@Entity
@Data
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Column(unique = true) private String email;
    private String telefone;
    private String senhaHash;
    @Enumerated(EnumType.STRING) private PapelUsuario papelPrincipal;
    private boolean ativo = true;
    private OffsetDateTime aceitouTermosEm;
    private OffsetDateTime criadoEm = OffsetDateTime.now();
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();
    private OffsetDateTime deletadoEm;
}
