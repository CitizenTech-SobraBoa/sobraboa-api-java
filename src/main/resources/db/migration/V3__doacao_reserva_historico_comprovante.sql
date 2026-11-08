-- =====================================================================
-- V3: doação, itens, reserva, histórico de status e comprovante
-- =====================================================================

-- DOAÇÃO e ITENS
CREATE TABLE doacao
(
    id                   BIGSERIAL PRIMARY KEY,
    doador_id            BIGINT        NOT NULL REFERENCES doador (usuario_id),
    endereco_retirada_id BIGINT        NOT NULL REFERENCES endereco (id),
    titulo               VARCHAR(150)  NOT NULL,
    descricao            TEXT,
    status               status_doacao NOT NULL DEFAULT 'RASCUNHO',
    janela_inicio        TIMESTAMPTZ   NOT NULL,
    janela_fim           TIMESTAMPTZ   NOT NULL,
    requer_refrigeracao  BOOLEAN       NOT NULL DEFAULT FALSE,
    instrucoes_retirada  VARCHAR(500),
    foto_url             VARCHAR(500),
    criado_em            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_doacao_janela CHECK (janela_fim > janela_inicio)
);

CREATE INDEX idx_doacao_status_janela ON doacao (status, janela_fim);
CREATE INDEX idx_doacao_doador ON doacao (doador_id);

CREATE TABLE doacao_item
(
    id               BIGSERIAL PRIMARY KEY,
    doacao_id        BIGINT         NOT NULL REFERENCES doacao (id) ON DELETE CASCADE,
    categoria_id     SMALLINT       NOT NULL REFERENCES categoria_alimento (id),
    descricao        VARCHAR(200)   NOT NULL,
    quantidade       NUMERIC(10, 2) NOT NULL CHECK (quantidade > 0),
    unidade          unidade_medida NOT NULL,
    peso_estimado_kg NUMERIC(10, 2) CHECK (peso_estimado_kg >= 0),
    data_validade    DATE,
    preparado_em     TIMESTAMPTZ
);

CREATE INDEX idx_doacao_item_doacao ON doacao_item (doacao_id);

-- RESERVA
CREATE TABLE reserva
(
    id                  BIGSERIAL PRIMARY KEY,
    doacao_id           BIGINT         NOT NULL REFERENCES doacao (id),
    receptor_id         BIGINT         NOT NULL REFERENCES receptor (usuario_id),
    status              status_reserva NOT NULL DEFAULT 'ATIVA',
    codigo_retirada     VARCHAR(8)     NOT NULL,
    reservada_em        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    expira_em           TIMESTAMPTZ    NOT NULL,
    retirada_em         TIMESTAMPTZ,
    recebida_em         TIMESTAMPTZ,
    motivo_cancelamento VARCHAR(300)
);

-- Regra: no máximo UMA reserva em andamento por doação
CREATE UNIQUE INDEX uq_reserva_em_andamento_por_doacao
    ON reserva (doacao_id) WHERE status IN ('ATIVA', 'RETIRADA');

CREATE INDEX idx_reserva_receptor ON reserva (receptor_id, status);

-- HISTÓRICO DE STATUS DA DOAÇÃO
CREATE TABLE doacao_status_historico
(
    id              BIGSERIAL PRIMARY KEY,
    doacao_id       BIGINT        NOT NULL REFERENCES doacao (id),
    status_anterior status_doacao,
    status_novo     status_doacao NOT NULL,
    alterado_por    BIGINT REFERENCES usuario (id), -- NULL = ação do sistema
    alterado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    observacao      VARCHAR(300)
);

CREATE INDEX idx_historico_doacao ON doacao_status_historico (doacao_id, alterado_em);

-- COMPROVANTE
CREATE TABLE comprovante
(
    id                 BIGSERIAL PRIMARY KEY,
    reserva_id         BIGINT      NOT NULL UNIQUE REFERENCES reserva (id),
    codigo_verificacao VARCHAR(20) NOT NULL UNIQUE,
    peso_total_kg      NUMERIC(10, 2),
    emitido_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    url_pdf            VARCHAR(500)
);