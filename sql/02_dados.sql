-- Dados ficticios. A preparacao executa este arquivo uma unica vez e de forma atomica.
-- Nao reaplica seed nem recalcula saldos quando ja existem empresas.
INSERT INTO financeiro.categoria(descricao,natureza) VALUES
 ('Venda de produtos','R'),('Aluguel recebido','R'),('Juros de investimento','R'),
 ('Energia eletrica','D'),('Agua','D'),('Combustivel','D');
-- @comando
INSERT INTO financeiro.empresa(cnpj,razao_social,endereco) VALUES
 ('12345678000190','Aurora Comercio Ltda',ROW('Rua','das Flores','123','Sala 2','Centro','Curitiba','PR','80000000')::financeiro.endereco_completo),
 ('98765432000110','Horizonte Servicos Ltda',ROW('Avenida','Brasil','456',NULL,'Jardim America','Londrina','PR','86010000')::financeiro.endereco_completo);
-- @comando
INSERT INTO financeiro.email_empresa(empresa_id,email)
 SELECT id, CASE WHEN cnpj='12345678000190' THEN 'contato@aurora.example' ELSE 'contato@horizonte.example' END FROM financeiro.empresa;
-- @comando
INSERT INTO financeiro.telefone_empresa(empresa_id,ddi,ddd,numero)
 SELECT id,'55',CASE WHEN cnpj='12345678000190' THEN '41' ELSE '43' END,'999990001' FROM financeiro.empresa;
-- @comando
INSERT INTO financeiro.conta(empresa_id,nome)
 SELECT e.id,c.nome FROM financeiro.empresa e CROSS JOIN (VALUES('Caixa'),('Conta bancaria')) AS c(nome);
-- @comando
INSERT INTO financeiro.lancamento(conta_id,categoria_id,data_lancamento,documento,observacao,valor)
 SELECT c.id,k.id,d.data_lancamento,d.documento,'Carga inicial ficticia',d.valor
 FROM financeiro.conta c CROSS JOIN (VALUES
  ('Venda de produtos',DATE '2026-11-01','INI-001',1000.00::numeric),
  ('Energia eletrica',DATE '2026-11-02','INI-002',120.00::numeric),
  ('Aluguel recebido',DATE '2026-11-03','INI-003',300.00::numeric),
  ('Agua',DATE '2026-11-04','INI-004',80.00::numeric),
  ('Juros de investimento',DATE '2026-11-05','INI-005',50.00::numeric)
 ) AS d(categoria,data_lancamento,documento,valor)
 JOIN financeiro.categoria k ON k.descricao=d.categoria;
-- @comando
UPDATE financeiro.conta c SET saldo_atual = c.saldo_inicial +
 (SELECT sum(CASE k.natureza WHEN 'R' THEN l.valor ELSE -l.valor END)
  FROM financeiro.lancamento l JOIN financeiro.categoria k ON k.id=l.categoria_id WHERE l.conta_id=c.id);
