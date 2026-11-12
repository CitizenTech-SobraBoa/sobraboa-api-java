# SobraBoa API

Backend RESTful que conecta comércios com sobra de alimentos a organizações que combatem a fome.
Global Solution 2026/2 — CitizenTech — FIAP | Java Advanced

> A API roda no plano gratuito do Render e "dorme" após cerca de 15 minutos sem uso. A primeira requisição depois disso
> pode levar até 1 a 2 minutos (cold start).

## Sumário

- Problema
- Solução
- Tecnologias
- Arquitetura
- Entidades e Relacionamentos
- Segurança (Spring Security + JWT)
- Flyway — versionamento de banco
- Fluxos de negócio (além do CRUD)
- Funcionalidade com IA (Spring AI)
- Mensageria (RabbitMQ)
- Endpoints
- Como Rodar
- Variáveis de ambiente
- Deploy online (Render)
- Contas de demonstração (perfil dev)
- Exemplos de Payload
- Testes
- Correspondência requisitos × implementação
- Decisões de projeto e limitações conhecidas
- Integrantes
- Estrutura do Projeto

---

## Problema

Todos os dias, padarias, restaurantes e hortifrútis descartam alimentos próprios para consumo, enquanto
organizações sociais (ONGs, abrigos, casas de acolhida) têm dificuldade para receber esses alimentos de forma
rápida, segura e organizada. Faltam um canal único para o comércio oferecer a sobra, regras claras de retirada
(horário, refrigeração, responsável) e um registro que comprove a doação. O resultado é desperdício de comida e
pessoas em insegurança alimentar sem acesso a ela.

## Solução

A SobraBoa API centraliza o ciclo completo da doação, com autenticação real e três perfis de acesso:

- Autenticação por token (Bearer/JWT), com três perfis: **DOADOR**, **RECEPTOR** e **ADMIN**
- Cadastro de doadores e receptores com **aprovação prévia do administrador**
- Cadastro de doações com itens, categorias de alimento e **janela de retirada**
- **Assistente de IA (Spring AI)** que monta o rascunho da doação a partir de um texto livre do doador
- Busca de doações **por proximidade** (latitude, longitude e raio), com paginação
- Reserva com **código de retirada**, confirmação pelo doador, confirmação de recebimento pelo receptor e
  **comprovante** com código de verificação
- Controle de concorrência: uma doação nunca é reservada por duas organizações ao mesmo tempo
- Expiração automática de reservas e doações fora da janela
- Histórico (linha do tempo) de todas as mudanças de status de cada doação
- **Eventos assíncronos com RabbitMQ** que geram notificações para doador e receptor
- Painel de métricas de impacto para o administrador (kg doados, doações por status, taxa de aproveitamento)
- Proteção contra força bruta no login (após 5 falhas seguidas, bloqueio temporário com HTTP 429)
- Versionamento de banco com Flyway, PostgreSQL, Docker, CI no GitHub Actions e deploy no Render

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Web (MVC) | — |
| Spring Data JPA (Hibernate) | — |
| Spring Security + JWT (jjwt) | 0.12.6 |
| Spring AI (OpenAI starter) | 2.0.1 |
| Spring AMQP (RabbitMQ) | gerenciado pelo Spring Boot |
| Flyway | gerenciado pelo Spring Boot |
| Bean Validation | — |
| Caffeine (controle de tentativas de login) | — |
| Lombok | — |
| SpringDoc OpenAPI / Swagger | 3.0.0 |
| Spring Actuator (health check) | — |
| PostgreSQL | 16 |
| RabbitMQ | 3 (imagem com painel de gerenciamento) |
| Maven (wrapper incluso) | — |
| Docker / Docker Compose | — |
| GitHub Actions (CI) | — |
| Render (deploy) | — |

## Arquitetura

```
 App Mobile ──HTTP/JSON (Bearer JWT)──▶  SobraBoa API (Spring Boot)
                                          │
                  ┌───────────────────────┼─────────────────────────┐
                  ▼                       ▼                         ▼
            PostgreSQL              Provedor de LLM            RabbitMQ
        (dados + Flyway)        (Spring AI: assistente)   exchange sobraboa.eventos
                                                                   │  doacao.#
                                                                   ▼
                                                      fila sobraboa.notificacoes ──▶ consumidor
                                                                   │ falha
                                                                   ▼
                                                      fila sobraboa.notificacoes.dlq
```

A API é organizada em pacotes por domínio (auth, doacao, reserva, admin, ia, messaging etc.), cada um com
controller, service, repositório, entidades e DTOs. Os DTOs são `records` com Bean Validation, e todo erro sai no mesmo
formato padronizado.

---

## Entidades e Relacionamentos

```
Usuario (DOADOR | RECEPTOR | ADMIN)
   ├── (1:1) Doador ──── (N) Doacao ──── (N) DoacaoItem ──── (1) CategoriaAlimento
   │                        │
   │                        ├── (N) Reserva ──── (1) Comprovante
   │                        └── (N) DoacaoStatusHistorico
   ├── (1:1) Receptor ── (N) Reserva
   └── (N) Notificacao

Doador, Receptor e Doacao (retirada) ──── (1) Endereco
```

| Entidade | Campos principais |
|---|---|
| Usuario | id, nome, email (único), telefone, senhaHash, papelPrincipal, ativo, aceitouTermosEm, deletadoEm |
| Doador | usuarioId, nomeFantasia, razaoSocial, cnpj, tipoEstabelecimento, endereco, statusAprovacao |
| Receptor | usuarioId, nomeOrganizacao, cnpj, tipo, pessoasAtendidas, possuiRefrigeracao, horarioFuncionamento, endereco, statusAprovacao |
| Endereco | logradouro, numero, complemento, bairro, cidade, uf, cep, latitude, longitude |
| CategoriaAlimento | id, nome, requerRefrigeracaoPadrao, observacaoCuidado |
| Doacao | id, doador, enderecoRetirada, titulo, descricao, status, janelaInicio, janelaFim, requerRefrigeracao, instrucoesRetirada, fotoUrl |
| DoacaoItem | doacao, categoria, descricao, quantidade, unidade, pesoEstimadoKg, dataValidade, preparadoEm |
| Reserva | id, doacao, receptor, status, codigoRetirada, reservadaEm, expiraEm, retiradaEm, recebidaEm, motivoCancelamento |
| DoacaoStatusHistorico | doacaoId, statusAnterior, statusNovo, alteradoPor (nulo = sistema), alteradoEm, observacao |
| Comprovante | reservaId (único), codigoVerificacao (único), pesoTotalKg, emitidoEm |
| Notificacao | eventoId, usuarioId, tipo, mensagem, lida, criadaEm |

Um Doador e um Receptor compartilham o identificador do seu Usuario (relação 1:1). O ADMIN não tem perfil associado.

---

## Segurança (Spring Security + JWT)

- Login com e-mail e senha (BCrypt). O token Bearer é assinado com HMAC-SHA256 e expira em 120 minutos por padrão.
- Sessão **stateless**, sem cookie e sem `JSESSIONID`. O CSRF fica desabilitado porque a proteção contra CSRF existe
  para o cenário em que o navegador envia um cookie de sessão automaticamente; como a API não usa sessão implícita,
  essa superfície de ataque não existe aqui.
- **Três papéis** (ADMIN, DOADOR, RECEPTOR), com rotas protegidas por papel no `SecurityConfig`.
- Cadastro público cria apenas DOADOR ou RECEPTOR, sempre **pendente de aprovação**. Não existe endpoint público para
  criar administrador: a conta é criada na subida da aplicação (`AdminSeeder`) a partir de variáveis de ambiente.
- A identidade do usuário nunca vem do corpo da requisição: é sempre extraída do token.
- Autorização em duas camadas: regras por papel nas rotas e **checagem de posse nos services** (o doador só mexe nas
  próprias doações, o receptor só nas próprias reservas, e o comprovante, o histórico e o detalhe da doação só aparecem
  para quem participa dela).
- O filtro JWT confere no banco, a cada requisição, se o usuário continua ativo e não foi excluído. O papel considerado
  é o do banco, não o do token.
- Limite de tentativas de login: após **5 falhas seguidas** para o mesmo e-mail, novos logins retornam
  `429 Too Many Requests` por 15 minutos (`LoginAttemptService`, em memória).
- Respostas `401` e `403` em JSON, no mesmo formato dos demais erros.
- CORS configurável por variável de ambiente (`CORS_ALLOWED_ORIGINS`).
- No perfil `prod` nenhum segredo tem valor padrão: se `JWT_SECRET` (mínimo de 32 caracteres), as credenciais do admin
  ou as do banco não existirem, a aplicação **não sobe** (fail-fast).
- O Swagger é controlado por `SWAGGER_ENABLED` e vem desligado por padrão em produção.

---

## Flyway — versionamento de banco

O schema é 100% gerenciado pelo Flyway (`src/main/resources/db/migration`). O Hibernate roda com `ddl-auto=none`
(o DDL usa tipos ENUM nativos do PostgreSQL); a aplicação nunca cria nem altera tabelas em runtime.

| Migration | Conteúdo |
|---|---|
| V1__tipos_usuarios_enderecos_perfis.sql | Tipos ENUM, usuário, endereço, doador e receptor |
| V2__categorias_alimento.sql | Categorias de alimento e carga inicial (8 categorias) |
| V3__doacao_reserva_historico_comprovante.sql | Doação, itens, reserva (com índice único parcial), histórico de status e comprovante |
| V4__notificacao.sql | Notificações geradas pelos eventos do RabbitMQ (com restrição de idempotência) |

Para criar uma nova migration, adicione `V5__descricao_curta.sql` na mesma pasta (numeração sempre crescente) e suba a
aplicação: o Flyway aplica na subida. Nunca edite uma migration já aplicada.

---

## Fluxos de negócio (além do CRUD)

### 1. Aprovação de cadastros
Doador e receptor se cadastram e ficam **PENDENTE**. O administrador lista os pendentes e aprova ou rejeita. Enquanto
não aprovado, o usuário consegue fazer login, mas não cria doações (doador) nem reserva (receptor).

### 2. Ciclo da doação

```
RASCUNHO ──publicar──▶ DISPONIVEL ──reservar──▶ RESERVADA ──retirada──▶ RETIRADA ──receber──▶ CONCLUIDA
                          ▲                        │
                          └── cancelar reserva ────┘   (ou reserva expirada, se a janela ainda estiver aberta)

DISPONIVEL ou RESERVADA ──janela termina──▶ EXPIRADA
RASCUNHO, DISPONIVEL ou RESERVADA ──cancelar──▶ CANCELADA
```

- O doador cria a doação (como rascunho), revisa e publica.
- A edição só é permitida em `RASCUNHO` ou `DISPONIVEL`.
- O cancelamento é lógico: a doação vai para `CANCELADA` e nada é apagado.
- Toda mudança de status é gravada no histórico, com quem fez e quando (ação do sistema aparece sem usuário).

### 3. Busca por proximidade
O receptor informa latitude, longitude e raio (padrão 5 km, máximo 50 km). Aparecem apenas doações `DISPONIVEL`, com a
hora atual dentro da janela de retirada e dentro do raio (distância pela fórmula de Haversine), ordenadas pela janela que
termina primeiro, com paginação.

### 4. Reserva sem concorrência
Ao reservar, a linha da doação é travada no banco (`PESSIMISTIC_WRITE`) e um **índice único parcial** garante no máximo
uma reserva em andamento por doação. Duas organizações disputando a mesma doação nunca conseguem reservar as duas: a
segunda recebe `409`. Outras regras: a hora atual precisa estar na janela, e doações que exigem refrigeração só podem ser
reservadas por receptores que possuem refrigeração. O prazo para retirar é de 120 minutos (configurável), limitado ao fim
da janela.

### 5. Retirada com código e comprovante
- Ao reservar, o receptor recebe um **código de 6 caracteres** (sem caracteres ambíguos como 0, O, 1 e I).
- O doador confirma a retirada informando o código (`RETIRADA`).
- O receptor confirma o recebimento (`CONCLUIDA`) e o sistema emite o **comprovante**, com código de verificação (`SB-...`)
  e o peso total estimado dos itens.
- O comprovante só é visível para o administrador, o receptor da reserva e o doador da doação.

### 6. Cancelamento de reserva
O receptor pode desistir de uma reserva ativa. A doação volta a ficar `DISPONIVEL` se a janela ainda estiver aberta
(senão vai para `EXPIRADA`), e o doador é avisado.

### 7. Expiração automática
O `ExpiracaoJob` roda a cada 60 segundos: reservas ativas vencidas viram `EXPIRADA` (a doação volta a `DISPONIVEL` se a
janela ainda estiver aberta) e doações cuja janela terminou viram `EXPIRADA`.

### 8. Métricas de impacto (admin)
Doações por status, total de kg doados, quantidade de doadores e receptores aprovados e taxa de aproveitamento.

---

## Funcionalidade com IA (Spring AI)

O **assistente de doação** faz parte do fluxo de cadastro, não é um chat solto: o doador descreve em texto livre o que
sobrou (por exemplo, "Sobraram 30 pães franceses e 2 bolos, os bolos precisam de geladeira") e a API devolve uma doação
**em rascunho**, pronta para revisão.

- A resposta do modelo é convertida para um objeto estruturado (título, descrição, refrigeração, instruções de retirada e
  lista de itens com categoria, quantidade, unidade, peso e validade).
- O prompt restringe o modelo às categorias cadastradas no banco (categoria desconhecida vira "Outros"), proíbe inventar
  validade e não permite promessas sobre a qualidade do alimento.
- A saída do modelo é tratada como entrada não confiável: passa por Bean Validation antes de ser salva, com título
  reserva (início do texto do doador) quando o modelo não devolve um.
- **Human-in-the-loop:** a IA só cria `RASCUNHO`. Quem publica é sempre o doador.
- Se o provedor estiver fora ou sem chave configurada, a API continua funcionando e apenas este endpoint responde `503`
  ("Cadastre a doação manualmente").
- A chamada ao modelo fica isolada em uma interface (`SugestaoGateway`), o que permite testar sem acessar o provedor.
- O provedor e o modelo são configuráveis por variáveis de ambiente (`OPENAI_API_KEY`, `OPENAI_MODEL`). Provedores com
  API compatível com a da OpenAI funcionam apontando `SPRING_AI_OPENAI_BASE_URL` para o endereço deles.

---

## Mensageria (RabbitMQ)

Quando uma reserva muda de estado, a API publica um evento no RabbitMQ **depois do commit** no banco
(`@TransactionalEventListener(AFTER_COMMIT)`), de forma que nenhuma notificação falsa é enviada se a transação falhar.

```
Requisição ──▶ ReservaService ──▶ commit ──▶ EventoPublisher ──▶ exchange "sobraboa.eventos" (topic)
                                                                      │  routing key "doacao.<tipo>"
                                                                      ▼
                                              fila "sobraboa.notificacoes" (binding "doacao.#")
                                                                      │
                                                                      ▼
                                           NotificacaoConsumer ──▶ grava Notificacao no banco
```

| Evento | Disparado quando | Notificação para |
|---|---|---|
| DOACAO_RESERVADA | receptor reserva | doador |
| RETIRADA_CONFIRMADA | doador confirma a retirada | receptor |
| DOACAO_CONCLUIDA | receptor confirma o recebimento | doador e receptor |
| RESERVA_CANCELADA | receptor cancela a reserva | doador |

- **Idempotência:** cada evento tem um `eventoId`, e a tabela de notificações tem restrição única por evento e usuário;
  uma mensagem repetida não gera aviso duplicado.
- **Resiliência:** o consumidor tenta 3 vezes; se falhar, a mensagem vai para a fila de mensagens mortas
  `sobraboa.notificacoes.dlq`. Se o RabbitMQ estiver fora do ar, a reserva continua funcionando (o erro é registrado em
  log) e apenas o aviso daquela operação se perde.
- O app Mobile consome o resultado pelas rotas de notificações (listar e marcar como lida).
- O painel de gerenciamento do RabbitMQ fica na porta 15672 (usuário e senha `guest` em ambiente local).

---

## Endpoints

### Autenticação (públicos)
| Método | Rota | Descrição |
|---|---|---|
| POST | /api/auth/registro/doador | Cadastro de doador (fica pendente de aprovação) |
| POST | /api/auth/registro/receptor | Cadastro de receptor (fica pendente de aprovação) |
| POST | /api/auth/login | Login, retorna o token Bearer |

### Doações
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | /api/doacoes | DOADOR aprovado | Criar doação (rascunho) |
| POST | /api/doacoes/assistente | DOADOR aprovado | Criar rascunho a partir de texto livre (IA) |
| GET | /api/doacoes/minhas | DOADOR | Listar as próprias doações (paginado) |
| GET | /api/doacoes/proximas | RECEPTOR | Doações disponíveis por proximidade (paginado) |
| GET | /api/doacoes/{id} | autenticado | Detalhe (visível para dono, admin, participante ou se estiver disponível) |
| PUT | /api/doacoes/{id} | DOADOR (dono) | Editar (só rascunho ou disponível) |
| PATCH | /api/doacoes/{id}/publicar | DOADOR (dono) | Publicar rascunho |
| DELETE | /api/doacoes/{id} | DOADOR (dono) | Cancelar (cancelamento lógico) |
| GET | /api/doacoes/{id}/historico | admin, dono ou receptor que reservou | Linha do tempo de status |

### Reservas
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| POST | /api/doacoes/{id}/reservas | RECEPTOR aprovado | Reservar (gera o código de retirada) |
| GET | /api/reservas/minhas | RECEPTOR | Listar as próprias reservas (paginado) |
| POST | /api/reservas/{id}/retirada | DOADOR (dono da doação) | Confirmar a retirada com o código |
| POST | /api/reservas/{id}/receber | RECEPTOR (dono) | Confirmar o recebimento (emite o comprovante) |
| POST | /api/reservas/{id}/cancelar | RECEPTOR (dono) | Cancelar reserva ativa |
| GET | /api/reservas/{id}/comprovante | admin, doador ou receptor da reserva | Comprovante da doação |

### Notificações
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | /api/notificacoes/minhas | autenticado | Avisos do usuário, do mais novo ao mais antigo (paginado) |
| PATCH | /api/notificacoes/{id}/lida | autenticado | Marcar como lida |

### Admin
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | /api/admin/doadores/pendentes | ADMIN | Cadastros de doadores aguardando decisão (paginado) |
| GET | /api/admin/receptores/pendentes | ADMIN | Cadastros de receptores aguardando decisão (paginado) |
| PATCH | /api/admin/doadores/{id}/aprovacao | ADMIN | Aprovar ou rejeitar doador |
| PATCH | /api/admin/receptores/{id}/aprovacao | ADMIN | Aprovar ou rejeitar receptor |
| GET | /api/admin/metricas | ADMIN | Indicadores de impacto |

### Apoio e infraestrutura
| Método | Rota | Perfil | Descrição |
|---|---|---|---|
| GET | /api/categorias | autenticado | Categorias de alimento (o app precisa dos ids para montar os itens) |
| GET | /actuator/health | público | Health check (usado pelo Render e pelo Docker) |
| GET | /swagger-ui.html | público | Documentação interativa (quando habilitada) |

### Formato padrão de erro

```json
{
  "timestamp": "2026-11-11T12:00:00Z",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos",
  "caminho": "/api/auth/login",
  "campos": [{ "campo": "email", "mensagem": "não deve estar em branco" }]
}
```
O campo `campos` só aparece nos erros de validação. Códigos usados: 400 dados inválidos, 401 sem token ou token
inválido, 403 sem permissão, 404 não encontrado, 409 conflito, 422 regra de negócio, 429 muitas tentativas de login e
503 IA indisponível.

---

## Como Rodar

### Pré-requisitos
- Java 21
- Docker e Docker Compose (para o PostgreSQL e o RabbitMQ)
- Maven não precisa estar instalado: o projeto inclui o Maven Wrapper (`mvnw`)

### Opção 1 — API pelo Maven, banco e RabbitMQ no Docker (recomendado para desenvolver)

```bash
docker compose up -d postgres rabbitmq
./mvnw spring-boot:run
```
A API sobe na porta `8080` com o perfil `dev` (padrão): o Flyway cria o schema, o `AdminSeeder` cria o administrador e o
`DemoDataInitializer` carrega os dados de demonstração. O PostgreSQL do compose escuta na porta `5433` do seu computador.

- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Painel do RabbitMQ: `http://localhost:15672` (guest / guest)

Para usar o assistente de IA, defina a chave antes de subir:
```bash
export OPENAI_API_KEY=sua-chave
```
Sem a chave, tudo funciona, exceto o endpoint do assistente, que responde `503`.

### Opção 2 — Tudo no Docker Compose (igual à produção)

```bash
cp .env.example .env     # defina JWT_SECRET (mínimo 32 caracteres) e ADMIN_SENHA
docker compose up -d --build
```
Para carregar os dados de demonstração nesse modo, suba com `API_PROFILES=prod,demo docker compose up -d --build`.
Para apagar o banco local e recomeçar: `docker compose down -v`.

Para gerar um segredo JWT: `openssl rand -base64 48`.

### Rodar os testes

```bash
docker compose up -d postgres
./mvnw verify
```
Os testes de integração usam o PostgreSQL real da porta `5433`. O RabbitMQ é simulado nos testes, então não precisa
estar no ar.

---

## Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| SPRING_PROFILES_ACTIVE | dev | `dev` (dados demo), `prod` (sem dados demo) ou `prod,demo` (produção com dados demo) |
| PORT | 8080 | Porta HTTP (o Render injeta esse valor) |
| DB_URL | — | URL JDBC do PostgreSQL (obrigatória em `prod`, ex.: `jdbc:postgresql://host:5432/banco`) |
| DB_USER / DB_PASSWORD | — | Credenciais do PostgreSQL (obrigatórias em `prod`) |
| RABBITMQ_HOST / RABBITMQ_PORT | localhost / 5672 | Endereço do RabbitMQ |
| RABBITMQ_USER / RABBITMQ_PASSWORD | guest / guest | Credenciais do RabbitMQ |
| RABBITMQ_VHOST | / | Virtual host (provedores em nuvem exigem um próprio) |
| RABBITMQ_SSL | false | `true` para conexão com TLS (porta 5671) |
| JWT_SECRET | (valor de dev) | Segredo HMAC do token, mínimo de 32 caracteres. Obrigatório em `prod` |
| JWT_EXPIRATION_MINUTES | 120 | Validade do token |
| ADMIN_EMAIL / ADMIN_SENHA | admin@sobraboa.com / Admin@12345 (dev) | Conta do administrador criada na subida. Obrigatórias em `prod` |
| RESERVA_MINUTOS | 120 | Prazo para retirar após reservar |
| OPENAI_API_KEY | — | Chave do provedor de LLM (sem ela, só o assistente de IA fica indisponível) |
| OPENAI_MODEL | gpt-4o-mini | Modelo usado pelo assistente |
| CORS_ALLOWED_ORIGINS | * (dev) | Origens permitidas, separadas por vírgula. Obrigatória em `prod` |
| SWAGGER_ENABLED | false em prod | Liga o Swagger UI e a documentação OpenAPI em produção |

Nenhum segredo real é commitado: o `.env` fica fora do repositório, e o `.env.example` traz apenas valores de exemplo.

---

## Deploy online (Render)

A aplicação roda como Web Service (Docker) no Render, com o PostgreSQL gerenciado do próprio Render e o RabbitMQ em um
provedor externo (o Render não oferece RabbitMQ gerenciado).

| Item | Como foi feito |
|---|---|
| Build | `Dockerfile` multi-stage (Maven com JDK 21 para compilar, depois JRE 21 com usuário não-root) |
| Banco | PostgreSQL gerenciado do Render, na mesma região do serviço |
| Mensageria | RabbitMQ gerenciado externo (plano gratuito), conectado por TLS na porta 5671 |
| Perfil | `SPRING_PROFILES_ACTIVE=prod,demo` (PostgreSQL com dados de demonstração para a avaliação) |
| Health check | `/actuator/health/liveness` |
| Migrations | O Flyway aplica V1 a V4 automaticamente na subida |
| CI | GitHub Actions (`.github/workflows/ci.yml`): build, testes e build da imagem Docker a cada push |

Passo a passo:

1. Criar o PostgreSQL no Render (plano gratuito) e anotar hostname, porta, banco, usuário e senha.
2. Criar a instância de RabbitMQ no provedor externo e anotar host, usuário, senha e virtual host.
3. Criar um Web Service no Render a partir do repositório, com linguagem Docker e o health check acima.
4. Definir as variáveis de ambiente: `SPRING_PROFILES_ACTIVE`, `DB_URL` (no formato `jdbc:postgresql://HOST:5432/BANCO`),
   `DB_USER`, `DB_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT=5671`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`,
   `RABBITMQ_VHOST`, `RABBITMQ_SSL=true`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_SENHA`, `OPENAI_API_KEY`,
   `CORS_ALLOWED_ORIGINS` e `SWAGGER_ENABLED=true`.
5. Para republicar, basta dar `git push` na branch `main`: o Render faz o build e o deploy automaticamente.

Observações do plano gratuito: o serviço hiberna após cerca de 15 minutos sem requisições (cold start de até 1 a 2
minutos) e o PostgreSQL gratuito expira 30 dias após a criação.

---

## Contas de demonstração (perfil dev)

Criadas apenas se o banco ainda não tiver doadores. **Senha de todas: `Senha@123`** (exceto o administrador).

| Perfil | E-mail | Situação |
|---|---|---|
| ADMIN | admin@sobraboa.com (senha `Admin@12345` no perfil dev) | ativo |
| DOADOR | padaria@sobraboa.com | aprovado (Padaria Boa Massa) |
| DOADOR | restaurante@sobraboa.com | aprovado (Sabor Caseiro) |
| DOADOR | hortifruti@sobraboa.com | **pendente** (para demonstrar a aprovação do admin) |
| RECEPTOR | ong@sobraboa.com | aprovado, com refrigeração (ONG Mesa Cheia) |
| RECEPTOR | casa@sobraboa.com | aprovado, sem refrigeração (Casa de Acolhida Esperança) |

Também são criadas cinco doações de exemplo: duas disponíveis, um rascunho, uma reservada (código de retirada `DEMO42`) e
uma concluída com comprovante. As janelas de tempo são relativas ao momento em que o banco foi populado; para recriar os
dados do zero, rode `docker compose down -v` e suba novamente.

---

## Exemplos de Payload

### POST /api/auth/registro/doador
```json
{
  "nome": "Carlos Pereira",
  "email": "doador@exemplo.com",
  "telefone": "11999998888",
  "senha": "Senha@123",
  "nomeFantasia": "Padaria Pão Quente",
  "razaoSocial": "Pão Quente Alimentos LTDA",
  "cnpj": "11.222.333/0001-81",
  "tipoEstabelecimento": "Padaria",
  "endereco": {
    "logradouro": "Rua das Flores", "numero": "100", "bairro": "Centro",
    "cidade": "São Paulo", "uf": "SP", "cep": "01000-000",
    "latitude": -23.55052, "longitude": -46.633308
  },
  "aceitouTermos": true
}
```

### POST /api/auth/registro/receptor
```json
{
  "nome": "João Souza",
  "email": "receptor@exemplo.com",
  "senha": "Senha@123",
  "nomeOrganizacao": "ONG Mesa Cheia",
  "tipo": "ONG",
  "pessoasAtendidas": 80,
  "possuiRefrigeracao": true,
  "horarioFuncionamento": "Seg a Sex, 8h às 17h",
  "endereco": {
    "logradouro": "Rua da Esperança", "numero": "200", "bairro": "Centro",
    "cidade": "São Paulo", "uf": "SP", "cep": "01001-000",
    "latitude": -23.551, "longitude": -46.634
  },
  "aceitouTermos": true
}
```

### POST /api/auth/login
```json
{ "email": "padaria@sobraboa.com", "senha": "Senha@123" }
```
Resposta: `token`, `tipo` ("Bearer"), `expiraEm`, `usuarioId`, `nome`, `papel` e `aprovacao`. Use o token no cabeçalho
`Authorization: Bearer <token>` e, no Swagger, cole apenas o token no botão **Authorize**.

### PATCH /api/admin/doadores/{id}/aprovacao
```json
{ "aprovado": true }
```

### POST /api/doacoes
```json
{
  "titulo": "Pães do dia",
  "descricao": "Sobra do fim do expediente",
  "janelaInicio": "2026-11-11T08:00:00-03:00",
  "janelaFim": "2026-11-12T20:00:00-03:00",
  "requerRefrigeracao": false,
  "instrucoesRetirada": "Retirar no balcão",
  "itens": [
    { "categoriaId": 1, "descricao": "Pão francês", "quantidade": 30, "unidade": "UN", "pesoEstimadoKg": 1.5 }
  ]
}
```

### POST /api/doacoes/assistente (IA)
```json
{
  "texto": "Sobraram 30 pães franceses e 2 bolos de cenoura hoje, os bolos precisam de geladeira",
  "janelaInicio": "2026-11-11T08:00:00-03:00",
  "janelaFim": "2026-11-12T20:00:00-03:00"
}
```

### POST /api/reservas/{id}/retirada
```json
{ "codigo": "ABC123" }
```

### POST /api/reservas/{id}/cancelar
```json
{ "motivo": "Sem transporte" }
```

---

## Testes

A suíte tem **19 testes**: 17 de integração de ponta a ponta (`FluxosIntegrationTest`, com MockMvc e PostgreSQL real) e
2 unitários da mensageria (`EventoPublisherTest`).

Cobertura principal:
- Segurança: rota sem token (401), senha errada (401), bloqueio após 5 tentativas (429), acesso por papel (403) e
  isolamento entre usuários (um receptor não enxerga o rascunho de outro doador)
- Validação e erros padronizados: JSON malformado, campos inválidos, rota inexistente, método não permitido e ordenação inválida
- Regra de aprovação: cadastro pendente não cria doação
- Fluxo completo: cadastro, aprovação, criação, publicação, reserva, conflito de segunda reserva, código de retirada
  errado e certo, recebimento, comprovante e histórico de status
- Cancelamento de reserva com a doação voltando a ficar disponível
- Assistente de IA (com o provedor simulado): criação do rascunho, título reserva, categoria desconhecida e indisponibilidade (503)
- Mensageria: publicação na exchange com a routing key certa e tolerância a falha do broker

```bash
docker compose up -d postgres
./mvnw verify
```

---

## Correspondência requisitos × implementação

| Requisito | Onde |
|---|---|
| 01. API REST do domínio (3+ entidades relacionadas, CRUD, paginação) | 11 entidades relacionadas; CRUD completo de doações; paginação em `minhas`, `proximas`, reservas, notificações e listagens do admin |
| 02. Validação e tratamento de erros | Bean Validation nos DTOs (records); `GlobalExceptionHandler` com `@RestControllerAdvice` e resposta padronizada (`ApiError`) |
| 03. Spring Security + JWT com 2+ papéis | Três papéis (DOADOR, RECEPTOR, ADMIN), rotas protegidas por papel e checagem de posse nos services |
| 04. Spring AI integrado ao fluxo | Assistente de doação que gera o rascunho a partir de texto livre, com saída estruturada, validação e revisão humana |
| 05. Arquitetura distribuída (opção A: mensageria) | RabbitMQ: evento publicado após o commit e consumido de forma assíncrona, com DLQ, retry e idempotência |
| 06. Documentação e integração | Swagger/OpenAPI com autenticação Bearer, este README, CORS configurável e API consumida pelo app Mobile |
| Extras | Flyway, Docker, CI no GitHub Actions, deploy no Render, bloqueio de login por tentativas, expiração automática, comprovante e métricas |

---

## Decisões de projeto e limitações conhecidas

- **Notificações sem canal externo:** o consumidor grava a notificação no banco e o app a consulta. Trocar por e-mail ou
  push exige alterar apenas o consumidor.
- **Produtor e consumidor na mesma aplicação:** a mensageria é assíncrona de verdade (o broker fica no meio), mas o
  consumidor não roda em um serviço separado. O consumidor poderia virar um serviço próprio sem mudar o contrato do evento.
- **Sem padrão outbox:** se o RabbitMQ estiver fora do ar no momento do commit, o aviso daquela operação se perde (a
  reserva em si não é afetada).
- **Expiração sem aviso:** quando uma reserva expira pelo job, nenhum evento é publicado.
- **Cancelar uma doação já reservada:** a doação vai para `CANCELADA`, mas a reserva associada não é alterada.
- **Bloqueio de login em memória:** o contador de tentativas não é compartilhado entre várias instâncias da API.
- **Janela de corrida rara:** a confirmação de retirada e o job de expiração não são serializados entre si.
- **Cadastro pendente pode fazer login:** o login devolve a situação de aprovação (`aprovacao`), e o app deve usá-la para
  orientar o usuário. Apenas as ações de negócio são bloqueadas.
- **Plano gratuito do Render:** cold start após inatividade e PostgreSQL com prazo de validade de 30 dias.

---

## Integrantes

| Nome | RM |
|---|---|
| Pedro Vaz Ferreira | 566551 |
| João Victor Luiz Oliveira Resende | 565139 |
| Vitor Dias dos Santos | 565422 |
| Felipe Kirschner Modesto | 561810 |

---

## Estrutura do Projeto

```
sobraboa-api/
├── pom.xml
├── README.md
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── mvnw / mvnw.cmd
├── .github/workflows/ci.yml
└── src/
    ├── main/
    │   ├── java/br/com/fiap/sobraboaapi/
    │   │   ├── SobraboaApiApplication.java
    │   │   ├── admin/          (aprovação de cadastros, métricas, AdminSeeder)
    │   │   ├── auth/           (registro e login)
    │   │   ├── categoria/      (categorias de alimento)
    │   │   ├── comprovante/    (emissão e consulta do comprovante)
    │   │   ├── config/         (OpenApiConfig, CorsConfig, DemoDataInitializer)
    │   │   ├── doacao/         (CRUD de doações, busca por proximidade, ExpiracaoJob)
    │   │   ├── doacaoitem/     (itens da doação)
    │   │   ├── doador/         (perfil do doador)
    │   │   ├── endereco/
    │   │   ├── exception/      (GlobalExceptionHandler, ApiError, exceções de domínio)
    │   │   ├── historico/      (linha do tempo de status)
    │   │   ├── ia/             (assistente de doação com Spring AI)
    │   │   ├── messaging/      (RabbitConfig, EventoPublisher, NotificacaoConsumer)
    │   │   ├── notificacao/    (notificações geradas pelos eventos)
    │   │   ├── receptor/       (perfil do receptor)
    │   │   ├── reserva/        (reserva, retirada, recebimento, cancelamento)
    │   │   ├── security/       (JWT, filtro, SecurityConfig, LoginAttemptService)
    │   │   ├── shared/         (enums e validações compartilhadas)
    │   │   └── usuario/
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       └── db/migration/   (Flyway, V1 a V4)
    └── test/
        ├── java/br/com/fiap/sobraboaapi/   (FluxosIntegrationTest, messaging/EventoPublisherTest)
        └── resources/application-test.properties
```
