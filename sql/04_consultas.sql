-- T4A - Tela 1, consulta 1: cabecalho. Substitua 1 pelo ID escolhido na tela.
SELECT c.id,e.razao_social,c.nome,c.saldo_inicial,c.saldo_atual
FROM financeiro.conta c JOIN financeiro.empresa e ON e.id=c.empresa_id WHERE c.id=1;

-- T4A - Tela 1, consulta 2: extrato e saldo acumulado em ordem cronologica.
SELECT l.data_lancamento,k.descricao,k.natureza,l.documento,l.valor,
 c.saldo_inicial + sum(CASE k.natureza WHEN 'R' THEN l.valor ELSE -l.valor END)
 OVER (ORDER BY l.data_lancamento,l.id ROWS UNBOUNDED PRECEDING) AS saldo
FROM financeiro.lancamento l JOIN financeiro.categoria k ON k.id=l.categoria_id
JOIN financeiro.conta c ON c.id=l.conta_id WHERE c.id=1 ORDER BY l.data_lancamento,l.id;

-- T4B(b): acesso SQL aos componentes do tipo composto.
SELECT id,cnpj,razao_social,(endereco).logradouro,(endereco).numero,
 (endereco).cidade,(endereco).uf FROM financeiro.empresa ORDER BY id;

-- Conferencia da carga minima: 2 empresas, 2 contas em cada uma, >=5 lancamentos por conta.
SELECT e.razao_social,c.nome,count(l.id) AS quantidade,c.saldo_atual
FROM financeiro.empresa e JOIN financeiro.conta c ON c.empresa_id=e.id
LEFT JOIN financeiro.lancamento l ON l.conta_id=c.id
GROUP BY e.id,e.razao_social,c.id,c.nome,c.saldo_atual ORDER BY e.id,c.id;
