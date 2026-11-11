CREATE TABLE notificacao
(
    id         BIGSERIAL PRIMARY KEY,
    evento_id  UUID         NOT NULL,
    usuario_id BIGINT       NOT NULL REFERENCES usuario (id),
    tipo       VARCHAR(40)  NOT NULL,
    mensagem   VARCHAR(300) NOT NULL,
    lida       BOOLEAN      NOT NULL DEFAULT FALSE,
    criada_em  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- idempotência: o mesmo evento não gera duas notificações para o mesmo usuário
    CONSTRAINT uq_notificacao_evento_usuario UNIQUE (evento_id, usuario_id)
);

CREATE INDEX idx_notificacao_usuario ON notificacao (usuario_id, criada_em DESC);