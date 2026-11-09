package br.com.fiap.sobraboaapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração: API completa + PostgreSQL real (docker compose up -d postgres).
 * O RabbitMQ é simulado (@MockitoBean), então não precisa estar no ar.
 * Cada teste roda numa transação que sofre rollback; os e-mails são únicos por execução.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxosIntegrationTest {

    @Autowired MockMvc mvc;
    @MockitoBean RabbitTemplate rabbit;

    @Value("${app.admin.email}") String adminEmail;
    @Value("${app.admin.senha}") String adminSenha;

    // ---------------------------------------------------------------- segurança / erros

    @Test
    void rotaProtegidaSemTokenRetorna401() throws Exception {
        mvc.perform(get("/api/doacoes/minhas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginComSenhaErradaRetorna401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(adminEmail, "senha-errada-123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sextaTentativaDeLoginErradaRetorna429() throws Exception {
        String email = unico("brute");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, "qualquer-senha"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "qualquer-senha")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validacaoRetorna400ComListaDeCampos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos").isArray());
    }

    @Test
    void rotaInexistenteRetorna404EmJson() throws Exception {
        mvc.perform(get("/api/rota-que-nao-existe").header("Authorization", "Bearer " + login(adminEmail, adminSenha)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void metodoNaoPermitidoRetorna405() throws Exception {
        mvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void apenasAdminAcessaPainelAdmin() throws Exception {
        String doador = registrarDoador();
        mvc.perform(get("/api/admin/metricas").header("Authorization", "Bearer " + login(doador, "Senha@123")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/metricas").header("Authorization", "Bearer " + login(adminEmail, adminSenha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kgDoados").exists());
    }

    @Test
    void doadorNaoPodeReservar() throws Exception {
        String doador = registrarDoador();
        mvc.perform(post("/api/doacoes/1/reservas").header("Authorization", "Bearer " + login(doador, "Senha@123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordenacaoInvalidaRetorna400() throws Exception {
        String doador = registrarDoador();
        mvc.perform(get("/api/doacoes/minhas?sort=campoQueNaoExiste")
                        .header("Authorization", "Bearer " + login(doador, "Senha@123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cadastroPendenteNaoPodeCriarDoacao() throws Exception {
        String doador = registrarDoador();                       // continua PENDENTE
        mvc.perform(post("/api/doacoes").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + login(doador, "Senha@123"))
                        .content(doacaoJson()))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- fluxo completo

    @Test
    void fluxoCompletoDaDoacaoAteOComprovante() throws Exception {
        String admin = login(adminEmail, adminSenha);
        String emailDoador = registrarDoador();
        String emailReceptor = registrarReceptor();

        aprovar(admin, "doadores", idPorLogin(emailDoador));
        aprovar(admin, "receptores", idPorLogin(emailReceptor));

        String tDoador = login(emailDoador, "Senha@123");
        String tReceptor = login(emailReceptor, "Senha@123");

        // doador cria o rascunho e publica
        String corpo = mvc.perform(post("/api/doacoes").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tDoador).content(doacaoJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RASCUNHO"))
                .andReturn().getResponse().getContentAsString();
        Integer doacaoId = JsonPath.read(corpo, "$.id");

        mvc.perform(patch("/api/doacoes/" + doacaoId + "/publicar").header("Authorization", "Bearer " + tDoador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));

        // receptor reserva
        String reserva = mvc.perform(post("/api/doacoes/" + doacaoId + "/reservas")
                        .header("Authorization", "Bearer " + tReceptor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVA"))
                .andReturn().getResponse().getContentAsString();
        Integer reservaId = JsonPath.read(reserva, "$.id");
        String codigo = JsonPath.read(reserva, "$.codigoRetirada");

        // segunda reserva da mesma doação -> 409
        mvc.perform(post("/api/doacoes/" + doacaoId + "/reservas").header("Authorization", "Bearer " + tReceptor))
                .andExpect(status().isConflict());

        // código errado -> 422; código certo -> RETIRADA
        mvc.perform(post("/api/reservas/" + reservaId + "/retirada").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tDoador).content("{\"codigo\":\"ZZZZZZ\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/reservas/" + reservaId + "/retirada").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tDoador).content("{\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRADA"));

        // receptor confirma o recebimento -> CONCLUIDA + comprovante
        mvc.perform(post("/api/reservas/" + reservaId + "/receber").header("Authorization", "Bearer " + tReceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"));

        mvc.perform(get("/api/reservas/" + reservaId + "/comprovante").header("Authorization", "Bearer " + tReceptor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoVerificacao", startsWith("SB-")));

        // trilha de auditoria: rascunho, publicada, reservada, retirada, concluída
        mvc.perform(get("/api/doacoes/" + doacaoId + "/historico").header("Authorization", "Bearer " + tDoador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(5)));
    }

    @Test
    void receptorPodeCancelarReservaEDoacaoVoltaAFicarDisponivel() throws Exception {
        String admin = login(adminEmail, adminSenha);
        String emailDoador = registrarDoador();
        String emailReceptor = registrarReceptor();
        aprovar(admin, "doadores", idPorLogin(emailDoador));
        aprovar(admin, "receptores", idPorLogin(emailReceptor));
        String tDoador = login(emailDoador, "Senha@123");
        String tReceptor = login(emailReceptor, "Senha@123");

        Integer doacaoId = JsonPath.read(mvc.perform(post("/api/doacoes").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tDoador).content(doacaoJson()))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(patch("/api/doacoes/" + doacaoId + "/publicar").header("Authorization", "Bearer " + tDoador));
        Integer reservaId = JsonPath.read(mvc.perform(post("/api/doacoes/" + doacaoId + "/reservas")
                        .header("Authorization", "Bearer " + tReceptor))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/reservas/" + reservaId + "/cancelar").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tReceptor).content("{\"motivo\":\"Sem transporte\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        mvc.perform(get("/api/doacoes/" + doacaoId).header("Authorization", "Bearer " + tReceptor))
                .andExpect(jsonPath("$.status").value("DISPONIVEL"));
    }

    // ---------------------------------------------------------------- helpers

    private String unico(String prefixo) { return prefixo + "-" + UUID.randomUUID() + "@teste.com"; }

    private String loginJson(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    private String login(String email, String senha) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, senha)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private Integer idPorLogin(String email) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(email, "Senha@123")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.usuarioId");
    }

    private void aprovar(String tokenAdmin, String tipo, Integer id) throws Exception {
        mvc.perform(patch("/api/admin/" + tipo + "/" + id + "/aprovacao").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenAdmin).content("{\"aprovado\":true}"))
                .andExpect(status().isNoContent());
    }

    private static final String ENDERECO = """
            {"logradouro":"Rua Teste","numero":"10","bairro":"Centro","cidade":"São Paulo","uf":"SP",
             "cep":"01000-000","latitude":-23.55052,"longitude":-46.633308}""";

    private String registrarDoador() throws Exception {
        String email = unico("doador");
        ResultActions r = mvc.perform(post("/api/auth/registro/doador").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nome":"Padaria Teste","email":"%s","senha":"Senha@123","nomeFantasia":"Padaria Teste",
                         "tipoEstabelecimento":"Padaria","endereco":%s,"aceitouTermos":true}""".formatted(email, ENDERECO)));
        r.andExpect(status().isCreated());
        return email;
    }

    private String registrarReceptor() throws Exception {
        String email = unico("receptor");
        mvc.perform(post("/api/auth/registro/receptor").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"ONG Teste","email":"%s","senha":"Senha@123","nomeOrganizacao":"ONG Teste",
                                 "tipo":"ONG","pessoasAtendidas":50,"possuiRefrigeracao":true,
                                 "endereco":%s,"aceitouTermos":true}""".formatted(email, ENDERECO)))
                .andExpect(status().isCreated());
        return email;
    }

    private String doacaoJson() {
        OffsetDateTime agora = OffsetDateTime.now();
        return """
                {"titulo":"Pães do dia","descricao":"Sobra do fim do expediente",
                 "janelaInicio":"%s","janelaFim":"%s","requerRefrigeracao":false,
                 "itens":[{"categoriaId":1,"descricao":"Pão francês","quantidade":30,"unidade":"UN","pesoEstimadoKg":1.5}]}"""
                .formatted(agora.minusHours(1), agora.plusHours(3));
    }
}