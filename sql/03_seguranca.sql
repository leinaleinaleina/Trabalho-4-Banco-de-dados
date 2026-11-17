-- T4C: tres logins reais, sem superusuario. Executado no banco bd_t4_individual.
-- Senhas NAO estao neste arquivo: PrepararBanco gera senhas aleatorias locais.
-- Na preparacao manual: apos criar os logins, configure as senhas com o pgAdmin.
DO $$ BEGIN
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='bdt4_ind_admin') THEN
  CREATE ROLE bdt4_ind_admin LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
 END IF;
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='bdt4_ind_leitor') THEN
  CREATE ROLE bdt4_ind_leitor LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
 END IF;
 IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='bdt4_ind_operador') THEN
  CREATE ROLE bdt4_ind_operador LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
 END IF;
END $$;
-- @comando
REVOKE ALL ON SCHEMA financeiro FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA financeiro FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA financeiro FROM PUBLIC;
REVOKE ALL ON TYPE financeiro.endereco_completo FROM PUBLIC;
GRANT CONNECT ON DATABASE bd_t4_individual TO bdt4_ind_admin,bdt4_ind_leitor,bdt4_ind_operador;
GRANT USAGE ON SCHEMA financeiro TO bdt4_ind_admin,bdt4_ind_leitor,bdt4_ind_operador;
GRANT USAGE ON TYPE financeiro.endereco_completo TO bdt4_ind_admin,bdt4_ind_leitor,bdt4_ind_operador;
-- @comando
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA financeiro TO bdt4_ind_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA financeiro TO bdt4_ind_admin;
GRANT CREATE ON SCHEMA financeiro TO bdt4_ind_admin;
-- @comando
GRANT SELECT ON ALL TABLES IN SCHEMA financeiro TO bdt4_ind_leitor,bdt4_ind_operador;
GRANT INSERT ON financeiro.empresa TO bdt4_ind_operador;
GRANT USAGE ON SEQUENCE financeiro.empresa_id_seq TO bdt4_ind_operador;
-- @comando
ALTER DEFAULT PRIVILEGES IN SCHEMA financeiro GRANT ALL ON TABLES TO bdt4_ind_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA financeiro GRANT ALL ON SEQUENCES TO bdt4_ind_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA financeiro GRANT SELECT ON TABLES TO bdt4_ind_leitor,bdt4_ind_operador;
