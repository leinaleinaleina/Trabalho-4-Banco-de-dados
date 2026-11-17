-- T4D: exemplos explicativos; no Java os parametros sao preenchidos via PreparedStatement.
-- Exemplo com a primeira conta e a categoria de vendas. O documento deve ser unico na conta.
BEGIN;
SELECT id FROM financeiro.conta WHERE id=1 FOR UPDATE;
INSERT INTO financeiro.lancamento(conta_id,categoria_id,data_lancamento,documento,observacao,valor)
SELECT 1,id,current_date,'MANUAL-001','Exemplo T4D',10.00
FROM financeiro.categoria WHERE descricao='Venda de produtos';
UPDATE financeiro.conta SET saldo_atual=saldo_atual+10.00 WHERE id=1;
COMMIT;
-- Para a variante intencional, use um documento diferente e troque COMMIT por ROLLBACK.
-- A variante abrupta e feita por ProcessoInterrompido.java, com Runtime.halt(23):
-- apos INSERT e UPDATE nao se chama commit(), rollback() nem close().
-- TransacaoService.java consulta o estado persistido por outra conexao.
