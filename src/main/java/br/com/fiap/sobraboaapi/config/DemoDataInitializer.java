package br.com.fiap.sobraboaapi.config;

import br.com.fiap.sobraboaapi.categoria.CategoriaAlimento;
import br.com.fiap.sobraboaapi.categoria.CategoriaAlimentoRepository;
import br.com.fiap.sobraboaapi.comprovante.Comprovante;
import br.com.fiap.sobraboaapi.comprovante.ComprovanteRepository;
import br.com.fiap.sobraboaapi.doacao.Doacao;
import br.com.fiap.sobraboaapi.doacao.DoacaoRepository;
import br.com.fiap.sobraboaapi.doacao.StatusDoacao;
import br.com.fiap.sobraboaapi.doacaoitem.DoacaoItem;
import br.com.fiap.sobraboaapi.doacaoitem.UnidadeMedida;
import br.com.fiap.sobraboaapi.doador.Doador;
import br.com.fiap.sobraboaapi.doador.DoadorRepository;
import br.com.fiap.sobraboaapi.endereco.Endereco;
import br.com.fiap.sobraboaapi.endereco.EnderecoRepository;
import br.com.fiap.sobraboaapi.historico.DoacaoStatusHistorico;
import br.com.fiap.sobraboaapi.historico.DoacaoStatusHistoricoRepository;
import br.com.fiap.sobraboaapi.receptor.Receptor;
import br.com.fiap.sobraboaapi.receptor.ReceptorRepository;
import br.com.fiap.sobraboaapi.reserva.Reserva;
import br.com.fiap.sobraboaapi.reserva.ReservaRepository;
import br.com.fiap.sobraboaapi.reserva.StatusReserva;
import br.com.fiap.sobraboaapi.shared.StatusAprovacao;
import br.com.fiap.sobraboaapi.usuario.PapelUsuario;
import br.com.fiap.sobraboaapi.usuario.Usuario;
import br.com.fiap.sobraboaapi.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Carrega dados de demonstração para o SobraBoa.
 * Só roda nos perfis "dev" e "demo" e só se o banco ainda não tiver doadores.
 *
 * Contas criadas (senha de todas: Senha@123):
 *   padaria@sobraboa.com      DOADOR   (aprovado)
 *   restaurante@sobraboa.com  DOADOR   (aprovado)
 *   hortifruti@sobraboa.com   DOADOR   (PENDENTE, para demonstrar a aprovação do admin)
 *   ong@sobraboa.com          RECEPTOR (aprovado, com refrigeração)
 *   casa@sobraboa.com         RECEPTOR (aprovado, sem refrigeração)
 * O admin é criado pelo AdminSeeder.
 */
@Component
@Profile({"dev", "demo"})
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements CommandLineRunner {

    private static final String SENHA_DEMO = "Senha@123";

    private final UsuarioRepository usuarioRepo;
    private final EnderecoRepository enderecoRepo;
    private final DoadorRepository doadorRepo;
    private final ReceptorRepository receptorRepo;
    private final CategoriaAlimentoRepository categoriaRepo;
    private final DoacaoRepository doacaoRepo;
    private final ReservaRepository reservaRepo;
    private final ComprovanteRepository comprovanteRepo;
    private final DoacaoStatusHistoricoRepository historicoRepo;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (doadorRepo.count() > 0) {
            log.info("Dados já existem, pulando inicialização de demonstração.");
            return;
        }
        log.info("==> [perfil dev] Carregando dados de demonstração do SobraBoa...");

        String hash = encoder.encode(SENHA_DEMO);
        OffsetDateTime agora = OffsetDateTime.now();

        // ---------- doadores ----------
        Doador padaria = criarDoador("Carlos Pereira", "padaria@sobraboa.com", hash,
                "Padaria Boa Massa", "Padaria Boa Massa Ltda", "11.111.111/0001-11", "Padaria",
                endereco("Av. Paulista", "1000", "Bela Vista", "São Paulo", "SP", "01310-100", "-23.561400", "-46.655900"),
                StatusAprovacao.APROVADO);
        Doador restaurante = criarDoador("Marta Lima", "restaurante@sobraboa.com", hash,
                "Sabor Caseiro", "Restaurante Sabor Caseiro Ltda", "22.222.222/0001-22", "Restaurante",
                endereco("Rua Augusta", "1500", "Consolação", "São Paulo", "SP", "01304-001", "-23.553000", "-46.648000"),
                StatusAprovacao.APROVADO);
        criarDoador("Paulo Nunes", "hortifruti@sobraboa.com", hash,
                "Hortifruti Verde Vida", "Verde Vida Comércio de Alimentos Ltda", "33.333.333/0001-33", "Hortifruti",
                endereco("Rua Vergueiro", "2000", "Vila Mariana", "São Paulo", "SP", "04102-000", "-23.589000", "-46.634000"),
                StatusAprovacao.PENDENTE);

        // ---------- receptores ----------
        Receptor ong = criarReceptor("João Souza", "ong@sobraboa.com", hash,
                "ONG Mesa Cheia", "44.444.444/0001-44", "ONG", 120, true, "Seg a Sex, 8h às 18h",
                endereco("Rua da Consolação", "800", "Consolação", "São Paulo", "SP", "01302-000", "-23.549000", "-46.647000"));
        criarReceptor("Ana Costa", "casa@sobraboa.com", hash,
                "Casa de Acolhida Esperança", "55.555.555/0001-55", "Abrigo", 45, false, "Todos os dias, 7h às 20h",
                endereco("Rua Vergueiro", "500", "Liberdade", "São Paulo", "SP", "01504-000", "-23.568000", "-46.636000"));

        // ---------- categorias (criadas pela migration V2) ----------
        CategoriaAlimento paes = categoria("Pães e massas");
        CategoriaAlimento horti = categoria("Hortifrúti");
        CategoriaAlimento pratos = categoria("Pratos prontos");
        CategoriaAlimento bebidas = categoria("Bebidas");

        // ---------- doações ----------
        // 1) DISPONIVEL: padaria, janela aberta agora
        Doacao paesDoDia = doacao(padaria, "Pães do dia", "Pães franceses e de forma assados hoje.",
                StatusDoacao.DISPONIVEL, agora.minusHours(1), agora.plusHours(5), false,
                "Retirar no balcão e falar com o gerente.",
                item(paes, "Pão francês", "30", UnidadeMedida.UN, "1.50", LocalDate.now()),
                item(paes, "Pão de forma", "8", UnidadeMedida.UN, "3.20", LocalDate.now().plusDays(1)));
        trilha(paesDoDia, padaria, StatusDoacao.RASCUNHO, StatusDoacao.DISPONIVEL);

        // 2) DISPONIVEL: restaurante, exige refrigeração
        Doacao marmitas = doacao(restaurante, "Marmitas do almoço", "Prato pronto de arroz, feijão e frango grelhado.",
                StatusDoacao.DISPONIVEL, agora.minusHours(2), agora.plusHours(4), true,
                "Entrada pela porta lateral. Levar caixa térmica.",
                item(pratos, "Marmita arroz, feijão e frango", "20", UnidadeMedida.PORCAO, "8.00", LocalDate.now()));
        trilha(marmitas, restaurante, StatusDoacao.RASCUNHO, StatusDoacao.DISPONIVEL);

        // 3) RASCUNHO: padaria
        Doacao bolos = doacao(padaria, "Bolos de ontem", "Bolos simples com aparência perfeita, só passaram do ponto de venda.",
                StatusDoacao.RASCUNHO, agora.plusHours(3), agora.plusHours(8), false, null,
                item(paes, "Bolo de fubá", "6", UnidadeMedida.UN, "4.50", LocalDate.now().plusDays(1)));
        trilha(bolos, padaria, StatusDoacao.RASCUNHO);

        // 4) RESERVADA: restaurante -> ONG (código de retirada DEMO42)
        Doacao saladas = doacao(restaurante, "Saladas e sucos", "Saladas frescas e sucos naturais do buffet.",
                StatusDoacao.RESERVADA, agora.minusHours(1), agora.plusHours(6), false,
                "Retirar no caixa.",
                item(horti, "Salada mista", "12", UnidadeMedida.PORCAO, "2.40", LocalDate.now()),
                item(bebidas, "Suco natural", "10", UnidadeMedida.LITRO, "10.00", LocalDate.now()));
        trilha(saladas, restaurante, StatusDoacao.RASCUNHO, StatusDoacao.DISPONIVEL, StatusDoacao.RESERVADA);
        reserva(saladas, ong, StatusReserva.ATIVA, "DEMO42", agora.plusHours(2), null, null);

        // 5) CONCLUIDA: padaria -> ONG, com comprovante (alimenta o dashboard de impacto)
        Doacao paesOntem = doacao(padaria, "Pães de ontem", "Pães franceses e de queijo do dia anterior.",
                StatusDoacao.CONCLUIDA, agora.minusDays(1).minusHours(4), agora.minusDays(1), false,
                "Retirar no balcão.",
                item(paes, "Pão francês", "40", UnidadeMedida.UN, "2.00", LocalDate.now().minusDays(1)),
                item(paes, "Pão de queijo", "5", UnidadeMedida.KG, "5.00", LocalDate.now().minusDays(1)));
        trilha(paesOntem, padaria, StatusDoacao.RASCUNHO, StatusDoacao.DISPONIVEL, StatusDoacao.RESERVADA,
                StatusDoacao.RETIRADA, StatusDoacao.CONCLUIDA);
        Reserva concluida = reserva(paesOntem, ong, StatusReserva.CONCLUIDA, "ABC234",
                agora.minusDays(1).minusHours(1), agora.minusDays(1).minusMinutes(50), agora.minusDays(1).minusMinutes(30));
        Comprovante comp = new Comprovante();
        comp.setReservaId(concluida.getId());
        comp.setCodigoVerificacao("SB-DEMO00000001");
        comp.setPesoTotalKg(new BigDecimal("7.00"));
        comprovanteRepo.save(comp);

        log.info("==> Dados de demonstração carregados: {} doadores, {} receptores, {} doações. Senha de todas as contas: '{}'.",
                doadorRepo.count(), receptorRepo.count(), doacaoRepo.count(), SENHA_DEMO);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private Usuario usuario(String nome, String email, String hash, PapelUsuario papel) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenhaHash(hash);
        u.setPapelPrincipal(papel);
        u.setAceitouTermosEm(OffsetDateTime.now());
        return usuarioRepo.save(u);
    }

    private Endereco endereco(String logradouro, String numero, String bairro, String cidade,
                              String uf, String cep, String lat, String lng) {
        Endereco e = new Endereco();
        e.setLogradouro(logradouro);
        e.setNumero(numero);
        e.setBairro(bairro);
        e.setCidade(cidade);
        e.setUf(uf);
        e.setCep(cep);
        e.setLatitude(new BigDecimal(lat));
        e.setLongitude(new BigDecimal(lng));
        return enderecoRepo.save(e);
    }

    private Doador criarDoador(String nome, String email, String hash, String fantasia, String razao,
                               String cnpj, String tipo, Endereco endereco, StatusAprovacao status) {
        Doador d = new Doador();
        d.setUsuario(usuario(nome, email, hash, PapelUsuario.DOADOR));
        d.setNomeFantasia(fantasia);
        d.setRazaoSocial(razao);
        d.setCnpj(cnpj);
        d.setTipoEstabelecimento(tipo);
        d.setEndereco(endereco);
        d.setStatusAprovacao(status);
        if (status == StatusAprovacao.APROVADO) d.setAprovadoEm(OffsetDateTime.now());
        return doadorRepo.save(d);
    }

    private Receptor criarReceptor(String nome, String email, String hash, String organizacao, String cnpj,
                                   String tipo, int pessoas, boolean refrigeracao, String horario, Endereco endereco) {
        Receptor r = new Receptor();
        r.setUsuario(usuario(nome, email, hash, PapelUsuario.RECEPTOR));
        r.setNomeOrganizacao(organizacao);
        r.setCnpj(cnpj);
        r.setTipo(tipo);
        r.setPessoasAtendidas(pessoas);
        r.setPossuiRefrigeracao(refrigeracao);
        r.setHorarioFuncionamento(horario);
        r.setEndereco(endereco);
        r.setStatusAprovacao(StatusAprovacao.APROVADO);
        r.setAprovadoEm(OffsetDateTime.now());
        return receptorRepo.save(r);
    }

    private CategoriaAlimento categoria(String nome) {
        return categoriaRepo.findByNomeIgnoreCase(nome)
                .orElseThrow(() -> new IllegalStateException("Categoria não encontrada (rodou a migration V2?): " + nome));
    }

    private DoacaoItem item(CategoriaAlimento categoria, String descricao, String quantidade,
                            UnidadeMedida unidade, String pesoKg, LocalDate validade) {
        DoacaoItem i = new DoacaoItem();
        i.setCategoria(categoria);
        i.setDescricao(descricao);
        i.setQuantidade(new BigDecimal(quantidade));
        i.setUnidade(unidade);
        i.setPesoEstimadoKg(new BigDecimal(pesoKg));
        i.setDataValidade(validade);
        return i;
    }

    private Doacao doacao(Doador doador, String titulo, String descricao, StatusDoacao status,
                          OffsetDateTime inicio, OffsetDateTime fim, boolean refrigeracao,
                          String instrucoes, DoacaoItem... itens) {
        Doacao d = new Doacao();
        d.setDoador(doador);
        d.setEnderecoRetirada(doador.getEndereco());
        d.setTitulo(titulo);
        d.setDescricao(descricao);
        d.setStatus(status);
        d.setJanelaInicio(inicio);
        d.setJanelaFim(fim);
        d.setRequerRefrigeracao(refrigeracao);
        d.setInstrucoesRetirada(instrucoes);
        for (DoacaoItem item : itens) {
            item.setDoacao(d);
            d.getItens().add(item);       // cascade ALL salva os itens junto
        }
        return doacaoRepo.save(d);
    }

    private Reserva reserva(Doacao doacao, Receptor receptor, StatusReserva status, String codigo,
                            OffsetDateTime expiraEm, OffsetDateTime retiradaEm, OffsetDateTime recebidaEm) {
        Reserva r = new Reserva();
        r.setDoacao(doacao);
        r.setReceptor(receptor);
        r.setStatus(status);
        r.setCodigoRetirada(codigo);
        r.setExpiraEm(expiraEm);
        r.setRetiradaEm(retiradaEm);
        r.setRecebidaEm(recebidaEm);
        return reservaRepo.save(r);
    }

    /** Grava o histórico: primeira entrada (null -> seq[0]) e depois cada transição da sequência. */
    private void trilha(Doacao doacao, Doador autor, StatusDoacao... sequencia) {
        StatusDoacao anterior = null;
        for (StatusDoacao novo : sequencia) {
            DoacaoStatusHistorico h = new DoacaoStatusHistorico();
            h.setDoacaoId(doacao.getId());
            h.setStatusAnterior(anterior);
            h.setStatusNovo(novo);
            h.setAlteradoPor(autor.getUsuarioId());
            h.setObservacao("Carga de demonstração");
            historicoRepo.save(h);
            anterior = novo;
        }
    }
}