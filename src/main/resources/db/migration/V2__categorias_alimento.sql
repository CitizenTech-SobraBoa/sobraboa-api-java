-- =====================================================================
-- V2: categorias de alimento (tabela de apoio + dados iniciais)
-- Estes dados são obrigatórios: a API e a IA classificam os itens com elas.
-- =====================================================================

CREATE TABLE categoria_alimento
(
    id                         SMALLSERIAL PRIMARY KEY,
    nome                       VARCHAR(80) NOT NULL UNIQUE,
    requer_refrigeracao_padrao BOOLEAN     NOT NULL DEFAULT FALSE,
    observacao_cuidado         VARCHAR(300)
);

INSERT INTO categoria_alimento (nome, requer_refrigeracao_padrao, observacao_cuidado)
VALUES ('Pães e massas', FALSE, NULL),
       ('Hortifrúti', FALSE, 'Retirar itens visivelmente deteriorados.'),
       ('Pratos prontos', TRUE, 'Manter refrigerado e consumir rapidamente.'),
       ('Laticínios e frios', TRUE, 'Manter na cadeia do frio.'),
       ('Carnes e pescados', TRUE, 'Manter na cadeia do frio.'),
       ('Não perecíveis', FALSE, NULL),
       ('Bebidas', FALSE, NULL),
       ('Outros', FALSE, NULL);