-- T4A: modelo unico do caso 1. T4B(a): tipo composto nativo, sem JSON.
CREATE SCHEMA IF NOT EXISTS financeiro;
-- @comando
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace
                 WHERE n.nspname='financeiro' AND t.typname='endereco_completo') THEN
    CREATE TYPE financeiro.endereco_completo AS (
      tipo_logradouro varchar(30), logradouro varchar(120), numero varchar(20),
      complemento varchar(80), bairro varchar(80), cidade varchar(80),
      uf char(2), cep varchar(8)
    );
  END IF;
END $$;
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.empresa (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  cnpj varchar(14) NOT NULL UNIQUE CHECK (cnpj ~ '^[0-9]{14}$'),
  razao_social varchar(150) NOT NULL CHECK (length(trim(razao_social)) > 0),
  endereco financeiro.endereco_completo NOT NULL,
  CONSTRAINT endereco_valido CHECK (
    (endereco).tipo_logradouro IS NOT NULL AND length(trim((endereco).tipo_logradouro)) > 0 AND
    (endereco).logradouro IS NOT NULL AND length(trim((endereco).logradouro)) > 0 AND
    (endereco).numero IS NOT NULL AND length(trim((endereco).numero)) > 0 AND
    (endereco).bairro IS NOT NULL AND length(trim((endereco).bairro)) > 0 AND
    (endereco).cidade IS NOT NULL AND length(trim((endereco).cidade)) > 0 AND
    (endereco).uf IS NOT NULL AND (endereco).uf ~ '^[A-Z]{2}$' AND
    (endereco).cep IS NOT NULL AND (endereco).cep ~ '^[0-9]{8}$'
  )
);
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.email_empresa (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  empresa_id bigint NOT NULL REFERENCES financeiro.empresa(id),
  email varchar(150) NOT NULL CHECK (email ~ '^[^ @]+@[^ @]+[.][^ @]+$'),
  UNIQUE (empresa_id, email)
);
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.telefone_empresa (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  empresa_id bigint NOT NULL REFERENCES financeiro.empresa(id),
  ddi varchar(3) NOT NULL CHECK (ddi ~ '^[0-9]{1,3}$'),
  ddd varchar(3) NOT NULL CHECK (ddd ~ '^[0-9]{2,3}$'),
  numero varchar(12) NOT NULL CHECK (numero ~ '^[0-9]{8,12}$'),
  UNIQUE (empresa_id, ddi, ddd, numero)
);
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.conta (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  empresa_id bigint NOT NULL REFERENCES financeiro.empresa(id),
  nome varchar(100) NOT NULL CHECK (length(trim(nome)) > 0),
  saldo_inicial numeric(14,2) NOT NULL DEFAULT 0,
  saldo_atual numeric(14,2) NOT NULL DEFAULT 0,
  UNIQUE (empresa_id, nome)
);
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.categoria (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  descricao varchar(100) NOT NULL UNIQUE,
  natureza char(1) NOT NULL CHECK (natureza IN ('R','D'))
);
-- @comando
CREATE TABLE IF NOT EXISTS financeiro.lancamento (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  conta_id bigint NOT NULL REFERENCES financeiro.conta(id),
  categoria_id bigint NOT NULL REFERENCES financeiro.categoria(id),
  data_lancamento date NOT NULL,
  documento varchar(60) NOT NULL CHECK (length(trim(documento)) > 0),
  observacao varchar(300) NOT NULL DEFAULT '',
  valor numeric(14,2) NOT NULL CHECK (valor > 0),
  criado_em timestamptz NOT NULL DEFAULT current_timestamp,
  UNIQUE (conta_id, documento)
);
-- @comando
CREATE INDEX IF NOT EXISTS idx_lancamento_extrato ON financeiro.lancamento(conta_id, data_lancamento, id);
-- @comando
COMMENT ON COLUMN financeiro.empresa.endereco IS 'T4B: endereco_completo, tipo composto nativo de oito campos; complemento opcional.';
