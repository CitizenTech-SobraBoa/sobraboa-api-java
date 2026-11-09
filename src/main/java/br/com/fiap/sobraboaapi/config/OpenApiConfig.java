package br.com.fiap.sobraboaapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String esquemaBearer = "bearerAuth";
        return new OpenAPI()
                // Habilita o botão "Authorize" do Swagger (os controllers protegidos usam @SecurityRequirement)
                .components(new Components().addSecuritySchemes(esquemaBearer,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /api/auth/login (informe só o token, sem 'Bearer ')")))
                .info(new Info()
                        .title("SobraBoa API")
                        .description(
                                "API RESTful que conecta **comércios com sobra de alimentos** a **organizações que " +
                                        "atendem pessoas em insegurança alimentar**. " +
                                        "Desenvolvida na Global Solution 2026/2 — FIAP (CitizenTech).\n\n" +
                                        "**Funcionalidades:**\n" +
                                        "- Autenticação JWT com perfis DOADOR, RECEPTOR e ADMIN\n" +
                                        "- Cadastro de doações com janela de retirada e busca por proximidade\n" +
                                        "- Assistente de IA (Spring AI) que monta o rascunho da doação a partir de texto livre\n" +
                                        "- Reserva, retirada com código, recebimento e comprovante\n" +
                                        "- Eventos no RabbitMQ que avisam doador e receptor de forma assíncrona\n" +
                                        "- Métricas de impacto para o administrador\n\n" +
                                        "**Para testar:** faça login em `POST /api/auth/login` " +
                                        "(ex.: `ong@sobraboa.com` / `Senha@123`), copie o `token` e clique em **Authorize**."
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pedro Vaz, João Victor, Vitor Dias & Felipe Modesto")
                                .name("CitizenTech"))
                        .license(new License().name("FIAP — Global Solution 2026/2")));
    }
}