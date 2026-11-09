package br.com.fiap.sobraboaapi.auth;

import br.com.fiap.sobraboaapi.doador.Doador;
import br.com.fiap.sobraboaapi.doador.DoadorRepository;
import br.com.fiap.sobraboaapi.doador.RegistroDoadorRequest;
import br.com.fiap.sobraboaapi.endereco.Endereco;
import br.com.fiap.sobraboaapi.endereco.EnderecoRepository;
import br.com.fiap.sobraboaapi.endereco.EnderecoRequest;
import br.com.fiap.sobraboaapi.exception.ConflitoException;
import br.com.fiap.sobraboaapi.receptor.Receptor;
import br.com.fiap.sobraboaapi.receptor.ReceptorRepository;
import br.com.fiap.sobraboaapi.receptor.RegistroReceptorRequest;
import br.com.fiap.sobraboaapi.security.JwtService;
import br.com.fiap.sobraboaapi.security.LoginAttemptService;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import br.com.fiap.sobraboaapi.usuario.PapelUsuario;
import br.com.fiap.sobraboaapi.usuario.Usuario;
import br.com.fiap.sobraboaapi.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarioRepo;
    private final EnderecoRepository enderecoRepo;
    private final DoadorRepository doadorRepo;
    private final ReceptorRepository receptorRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService tentativas;

    @Transactional
    public Long registrarDoador(RegistroDoadorRequest r) {
        Usuario u = novoUsuario(r.nome(), r.email(), r.telefone(), r.senha(), PapelUsuario.DOADOR);
        Doador d = new Doador();
        d.setUsuario(u);
        d.setNomeFantasia(r.nomeFantasia());
        d.setRazaoSocial(r.razaoSocial());
        d.setCnpj(r.cnpj());
        d.setTipoEstabelecimento(r.tipoEstabelecimento());
        d.setEndereco(salvarEndereco(r.endereco()));
        return doadorRepo.save(d).getUsuarioId();
    }

    @Transactional
    public Long registrarReceptor(RegistroReceptorRequest r) {
        Usuario u = novoUsuario(r.nome(), r.email(), r.telefone(), r.senha(), PapelUsuario.RECEPTOR);
        Receptor rc = new Receptor();
        rc.setUsuario(u);
        rc.setNomeOrganizacao(r.nomeOrganizacao());
        rc.setCnpj(r.cnpj());
        rc.setTipo(r.tipo());
        rc.setPessoasAtendidas(r.pessoasAtendidas());
        rc.setPossuiRefrigeracao(r.possuiRefrigeracao());
        rc.setHorarioFuncionamento(r.horarioFuncionamento());
        rc.setEndereco(salvarEndereco(r.endereco()));
        return receptorRepo.save(rc).getUsuarioId();
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        tentativas.exigirNaoBloqueado(email);                       // 429 após 5 falhas seguidas

        Optional<Usuario> achado = usuarioRepo.findByEmail(email)
                .filter(x -> x.isAtivo() && x.getDeletadoEm() == null)
                .filter(x -> encoder.matches(req.senha(), x.getSenhaHash()));
        if (achado.isEmpty()) {
            tentativas.registrarFalha(email);
            throw new BadCredentialsException("Credenciais inválidas");
        }
        tentativas.limpar(email);
        Usuario u = achado.get();
        StatusAprovacao aprov = switch (u.getPapelPrincipal()) {
            case DOADOR -> doadorRepo.findById(u.getId()).map(Doador::getStatusAprovacao).orElse(null);
            case RECEPTOR -> receptorRepo.findById(u.getId()).map(Receptor::getStatusAprovacao).orElse(null);
            case ADMIN -> StatusAprovacao.APROVADO;
        };
        return new TokenResponse(jwt.gerar(u), "Bearer", jwt.expiraEm(), u.getId(), u.getNome(), u.getPapelPrincipal(), aprov);
    }

    private Usuario novoUsuario(String nome, String email, String tel, String senha, PapelUsuario papel) {
        String emailNormalizado = email.trim().toLowerCase();       // e-mail único sem diferenciar maiúsculas
        if (usuarioRepo.existsByEmail(emailNormalizado)) throw new ConflitoException("E-mail já cadastrado");
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(emailNormalizado);
        u.setTelefone(tel);
        u.setSenhaHash(encoder.encode(senha));
        u.setPapelPrincipal(papel);
        u.setAceitouTermosEm(OffsetDateTime.now());
        return usuarioRepo.save(u);
    }

    private Endereco salvarEndereco(EnderecoRequest e) {
        Endereco end = new Endereco();
        end.setLogradouro(e.logradouro());
        end.setNumero(e.numero());
        end.setComplemento(e.complemento());
        end.setBairro(e.bairro());
        end.setCidade(e.cidade());
        end.setUf(e.uf().toUpperCase());
        end.setCep(e.cep());
        end.setLatitude(e.latitude());
        end.setLongitude(e.longitude());
        return enderecoRepo.save(end);
    }
}