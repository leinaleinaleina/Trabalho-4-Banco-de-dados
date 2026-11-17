-- Acrescenta as oito tabelas ausentes. Nao renomeia tabelas nem altera endereco_completo.
CREATE TABLE IF NOT EXISTS financeiro.uf (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sigla char(2) NOT NULL UNIQUE CHECK (sigla ~ '^[A-Z]{2}$')
);
CREATE TABLE IF NOT EXISTS financeiro.cidade (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nome varchar(80) NOT NULL CHECK (length(trim(nome))>0),
 uf_id bigint NOT NULL REFERENCES financeiro.uf(id), UNIQUE(nome,uf_id)
);
CREATE TABLE IF NOT EXISTS financeiro.bairro (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nome varchar(80) NOT NULL UNIQUE CHECK (length(trim(nome))>0)
);
CREATE TABLE IF NOT EXISTS financeiro.tipo_logradouro (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nome varchar(30) NOT NULL UNIQUE CHECK (length(trim(nome))>0)
);
CREATE TABLE IF NOT EXISTS financeiro.logradouro (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nome varchar(120) NOT NULL CHECK (length(trim(nome))>0),
 tipo_logradouro_id bigint NOT NULL REFERENCES financeiro.tipo_logradouro(id), UNIQUE(nome,tipo_logradouro_id)
);
CREATE TABLE IF NOT EXISTS financeiro.endereco (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 cep varchar(8) NOT NULL CHECK (cep ~ '^[0-9]{8}$'),
 bairro_id bigint NOT NULL REFERENCES financeiro.bairro(id),
 cidade_id bigint NOT NULL REFERENCES financeiro.cidade(id),
 logradouro_id bigint NOT NULL REFERENCES financeiro.logradouro(id),
 UNIQUE(cep,bairro_id,cidade_id,logradouro_id)
);
CREATE TABLE IF NOT EXISTS financeiro.ddi (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo varchar(3) NOT NULL UNIQUE CHECK (codigo ~ '^[0-9]{1,3}$')
);
CREATE TABLE IF NOT EXISTS financeiro.ddd (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo varchar(3) NOT NULL UNIQUE CHECK (codigo ~ '^[0-9]{2,3}$')
);
-- @comando
CREATE OR REPLACE FUNCTION financeiro.resolver_endereco(a financeiro.endereco_completo)
RETURNS bigint LANGUAGE plpgsql SECURITY INVOKER SET search_path=financeiro,pg_temp AS $$
DECLARE v_uf bigint; v_cidade bigint; v_bairro bigint; v_tipo bigint; v_logradouro bigint; v_endereco bigint;
BEGIN
 IF a.tipo_logradouro IS NULL OR length(trim(a.tipo_logradouro))=0 OR a.logradouro IS NULL OR length(trim(a.logradouro))=0
 OR a.numero IS NULL OR length(trim(a.numero))=0 OR a.bairro IS NULL OR length(trim(a.bairro))=0
 OR a.cidade IS NULL OR length(trim(a.cidade))=0 OR a.uf IS NULL OR a.uf !~ '^[A-Z]{2}$'
 OR a.cep IS NULL OR a.cep !~ '^[0-9]{8}$' THEN
  RAISE EXCEPTION 'Endereco viola a restricao endereco_valido.' USING ERRCODE='23514';
 END IF;
 SELECT id INTO v_uf FROM financeiro.uf WHERE sigla=a.uf;
 IF v_uf IS NULL THEN INSERT INTO financeiro.uf(sigla) VALUES(a.uf) ON CONFLICT(sigla) DO UPDATE SET sigla=EXCLUDED.sigla RETURNING id INTO v_uf; END IF;
 SELECT id INTO v_cidade FROM financeiro.cidade WHERE nome=a.cidade AND uf_id=v_uf;
 IF v_cidade IS NULL THEN INSERT INTO financeiro.cidade(nome,uf_id) VALUES(a.cidade,v_uf) ON CONFLICT(nome,uf_id) DO UPDATE SET nome=EXCLUDED.nome RETURNING id INTO v_cidade; END IF;
 SELECT id INTO v_bairro FROM financeiro.bairro WHERE nome=a.bairro;
 IF v_bairro IS NULL THEN INSERT INTO financeiro.bairro(nome) VALUES(a.bairro) ON CONFLICT(nome) DO UPDATE SET nome=EXCLUDED.nome RETURNING id INTO v_bairro; END IF;
 SELECT id INTO v_tipo FROM financeiro.tipo_logradouro WHERE nome=a.tipo_logradouro;
 IF v_tipo IS NULL THEN INSERT INTO financeiro.tipo_logradouro(nome) VALUES(a.tipo_logradouro) ON CONFLICT(nome) DO UPDATE SET nome=EXCLUDED.nome RETURNING id INTO v_tipo; END IF;
 SELECT id INTO v_logradouro FROM financeiro.logradouro WHERE nome=a.logradouro AND tipo_logradouro_id=v_tipo;
 IF v_logradouro IS NULL THEN INSERT INTO financeiro.logradouro(nome,tipo_logradouro_id) VALUES(a.logradouro,v_tipo) ON CONFLICT(nome,tipo_logradouro_id) DO UPDATE SET nome=EXCLUDED.nome RETURNING id INTO v_logradouro; END IF;
 SELECT id INTO v_endereco FROM financeiro.endereco WHERE cep=a.cep AND bairro_id=v_bairro AND cidade_id=v_cidade AND logradouro_id=v_logradouro;
 IF v_endereco IS NULL THEN INSERT INTO financeiro.endereco(cep,bairro_id,cidade_id,logradouro_id) VALUES(a.cep,v_bairro,v_cidade,v_logradouro) ON CONFLICT(cep,bairro_id,cidade_id,logradouro_id) DO UPDATE SET cep=EXCLUDED.cep RETURNING id INTO v_endereco; END IF;
 RETURN v_endereco;
END $$;
-- @comando
ALTER TABLE financeiro.empresa ADD COLUMN IF NOT EXISTS endereco_id bigint;
CREATE OR REPLACE FUNCTION financeiro.vincular_endereco_empresa() RETURNS trigger
LANGUAGE plpgsql SECURITY INVOKER SET search_path=financeiro,pg_temp AS $$
BEGIN
 NEW.endereco_id=financeiro.resolver_endereco(NEW.endereco);
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS vincular_endereco_empresa ON financeiro.empresa;
CREATE TRIGGER vincular_endereco_empresa BEFORE INSERT OR UPDATE OF endereco,endereco_id ON financeiro.empresa
 FOR EACH ROW EXECUTE FUNCTION financeiro.vincular_endereco_empresa();
UPDATE financeiro.empresa SET endereco=endereco WHERE endereco_id IS NULL;
ALTER TABLE financeiro.empresa ALTER COLUMN endereco_id SET NOT NULL;
DO $$ BEGIN
 IF NOT EXISTS(SELECT FROM pg_constraint WHERE conrelid='financeiro.empresa'::regclass AND conname='empresa_endereco_fk') THEN
  ALTER TABLE financeiro.empresa ADD CONSTRAINT empresa_endereco_fk FOREIGN KEY(endereco_id) REFERENCES financeiro.endereco(id);
 END IF;
END $$;
-- @comando
INSERT INTO financeiro.ddi(codigo) SELECT DISTINCT ddi FROM financeiro.telefone_empresa ON CONFLICT DO NOTHING;
INSERT INTO financeiro.ddd(codigo) SELECT DISTINCT ddd FROM financeiro.telefone_empresa ON CONFLICT DO NOTHING;
CREATE OR REPLACE FUNCTION financeiro.vincular_codigos_telefone() RETURNS trigger
LANGUAGE plpgsql SECURITY INVOKER SET search_path=financeiro,pg_temp AS $$
BEGIN
 IF NOT EXISTS(SELECT FROM financeiro.ddi WHERE codigo=NEW.ddi) THEN
  INSERT INTO financeiro.ddi(codigo) VALUES(NEW.ddi) ON CONFLICT DO NOTHING;
 END IF;
 IF NOT EXISTS(SELECT FROM financeiro.ddd WHERE codigo=NEW.ddd) THEN
  INSERT INTO financeiro.ddd(codigo) VALUES(NEW.ddd) ON CONFLICT DO NOTHING;
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS vincular_codigos_telefone ON financeiro.telefone_empresa;
CREATE TRIGGER vincular_codigos_telefone BEFORE INSERT OR UPDATE OF ddi,ddd ON financeiro.telefone_empresa
 FOR EACH ROW EXECUTE FUNCTION financeiro.vincular_codigos_telefone();
DO $$ BEGIN
 IF NOT EXISTS(SELECT FROM pg_constraint WHERE conrelid='financeiro.telefone_empresa'::regclass AND conname='telefone_ddi_fk') THEN
  ALTER TABLE financeiro.telefone_empresa ADD CONSTRAINT telefone_ddi_fk FOREIGN KEY(ddi) REFERENCES financeiro.ddi(codigo);
 END IF;
 IF NOT EXISTS(SELECT FROM pg_constraint WHERE conrelid='financeiro.telefone_empresa'::regclass AND conname='telefone_ddd_fk') THEN
  ALTER TABLE financeiro.telefone_empresa ADD CONSTRAINT telefone_ddd_fk FOREIGN KEY(ddd) REFERENCES financeiro.ddd(codigo);
 END IF;
END $$;
