-- =====================================================================
-- V1: tipos (ENUMs), usuario, endereco e perfis (doador / receptor)
-- SobraBoa MVP · PostgreSQL 13+ (sem PostGIS)
-- =====================================================================

-- 1. TIPOS (ENUMS)
CREATE TYPE papel_usuario AS ENUM ('DOADOR', 'RECEPTOR', 'ADMIN');
CREATE TYPE status_aprovacao AS ENUM ('PENDENTE', 'APROVADO', 'REJEITADO');
CREATE TYPE status_doacao AS ENUM ('RASCUNHO', 'DISPONIVEL', 'RESERVADA', 'RETIRADA', 'CONCLUIDA', 'EXPIRADA', 'CANCELADA');
CREATE TYPE status_reserva AS ENUM ('ATIVA', 'RETIRADA', 'CONCLUIDA', 'CANCELADA', 'EXPIRADA');
CREATE TYPE unidade_medida AS ENUM ('UN', 'KG', 'PORCAO', 'CAIXA', 'LITRO');

-- 2. USUÁRIO
CREATE TABLE usuario
(
    id                BIGSERIAL PRIMARY KEY,
    nome              VARCHAR(150)  NOT NULL,
    email             VARCHAR(255)  NOT NULL UNIQUE,
    telefone          VARCHAR(20),
    senha_hash        VARCHAR(255)  NOT NULL,
    papel_principal   papel_usuario NOT NULL,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    aceitou_termos_em TIMESTAMPTZ,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    atualizado_em     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deletado_em       TIMESTAMPTZ
);

-- 3. ENDEREÇO (latitude/longitude em vez de PostGIS)
CREATE TABLE endereco
(
    id          BIGSERIAL PRIMARY KEY,
    logradouro  VARCHAR(200)  NOT NULL,
    numero      VARCHAR(20),
    complemento VARCHAR(100),
    bairro      VARCHAR(100),
    cidade      VARCHAR(100)  NOT NULL,
    uf          CHAR(2)       NOT NULL,
    cep         VARCHAR(9),
    latitude    NUMERIC(9, 6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude   NUMERIC(9, 6) NOT NULL CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_endereco_lat_lng ON endereco (latitude, longitude);

-- 4. PERFIS: DOADOR e RECEPTOR
CREATE TABLE doador
(
    usuario_id           BIGINT PRIMARY KEY REFERENCES usuario (id),
    razao_social         VARCHAR(200),
    nome_fantasia        VARCHAR(200)     NOT NULL,
    cnpj                 VARCHAR(18) UNIQUE,
    tipo_estabelecimento VARCHAR(60),
    endereco_id          BIGINT           NOT NULL REFERENCES endereco (id),
    status_aprovacao     status_aprovacao NOT NULL DEFAULT 'PENDENTE',
    aprovado_por         BIGINT REFERENCES usuario (id),
    aprovado_em          TIMESTAMPTZ
);

CREATE TABLE receptor
(
    usuario_id            BIGINT PRIMARY KEY REFERENCES usuario (id),
    nome_organizacao      VARCHAR(200)     NOT NULL,
    cnpj                  VARCHAR(18) UNIQUE,
    tipo                  VARCHAR(60),
    endereco_id           BIGINT           NOT NULL REFERENCES endereco (id),
    pessoas_atendidas     INT CHECK (pessoas_atendidas >= 0),
    possui_refrigeracao   BOOLEAN          NOT NULL DEFAULT FALSE,
    horario_funcionamento VARCHAR(200),
    status_aprovacao      status_aprovacao NOT NULL DEFAULT 'PENDENTE',
    aprovado_por          BIGINT REFERENCES usuario (id),
    aprovado_em           TIMESTAMPTZ
);