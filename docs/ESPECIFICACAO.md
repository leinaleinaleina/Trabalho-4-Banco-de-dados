# Modelo unico - caso 1

Autores: Moises S e Thaynara Nascimento de Jesus.

## T4A - modelo logico e normalizacao

O banco possui 14 tabelas fisicas: empresa, email_empresa, telefone_empresa, conta, categoria, lancamento, bairro, uf, tipo_logradouro, ddd, ddi, cidade, logradouro e endereco. Foram preservados os nomes das seis tabelas anteriores. No MER de referencia, Conta_Bancaria corresponde a conta; TipoTransacao a categoria; Transacao a lancamento; FoneEmpresa a telefone_empresa; EmailEmpresa a email_empresa. As outras oito entidades possuem tabelas homonimas, sem acentos.

A parte relacional separa os cadastros: cidade referencia UF; logradouro referencia Tipo_logradouro; Endereco referencia Bairro, Cidade e Logradouro; Empresa referencia Endereco; telefone referencia Empresa, DDI e DDD. As referencias telefonicas usam codigo, uma chave candidata UNIQUE; as demais usam a PK id. Conta e contatos referenciam Empresa. Lancamento referencia Conta e Categoria, sem repetir o nome da empresa ou a descricao/natureza da categoria.

Na extensao objeto-relacional T4B, Empresa conserva endereco_completo com os oito campos originais. Essa representacao estruturada coexiste com a referencia relacional endereco_id, conforme o escopo adotado. Nao se afirma que um composto e escalar na definicao classica de 1FN ou que a repeticao de localizacao no composto seja parte da decomposicao relacional normalizada. Uma unica modelagem explicita as tabelas e essa extensao; nao existe empresa_complexa paralela.

Em cada INSERT/UPDATE do endereco da Empresa, um trigger resolve/reutiliza os cadastros relacionais e preenche endereco_id a partir do composto, sem modificar o valor do tipo. No cadastro de telefone, outro trigger cadastra os codigos ausentes antes da validacao das FKs. As funcoes usam SECURITY INVOKER: nao promovem os logins do T4C. No teste do operador, usa-se um endereco existente para exercitar somente o INSERT na Empresa; os cadastros auxiliares nao mudam. A atualizacao de um banco antigo faz o preenchimento inicial na mesma transacao e preserva os registros originais. Manutencoes manuais de nomes dos cadastros exigem considerar tambem os valores compostos existentes; o menu nao oferece esse fluxo de alteracao.

`saldo_atual` e um agregado materializado exigido pelo caso: inicialmente equivale a saldo_inicial mais receitas menos despesas. Nao se guarda o saldo de cada linha em lancamento; ele e calculado pela consulta de extrato com SUM OVER. Valores monetarios sao NUMERIC(14,2), nunca FLOAT, e datas usam DATE.

## Dicionario de dados

| Tabela | Atributos |
|---|---|
| empresa | id BIGINT identity PK; cnpj VARCHAR(14) NOT NULL UNIQUE; razao_social VARCHAR(150) NOT NULL; endereco endereco_completo NOT NULL; endereco_id FK NOT NULL. |
| email_empresa | id BIGINT identity PK; empresa_id BIGINT FK NOT NULL; email VARCHAR(150) NOT NULL; UNIQUE(empresa_id,email). |
| telefone_empresa | id BIGINT identity PK; empresa_id BIGINT FK NOT NULL; ddi VARCHAR(3) FK para ddi.codigo; ddd VARCHAR(3) FK para ddd.codigo; numero VARCHAR(12); todos NOT NULL; UNIQUE(empresa_id,ddi,ddd,numero). |
| conta | id BIGINT identity PK; empresa_id BIGINT FK NOT NULL; nome VARCHAR(100) NOT NULL; saldo_inicial e saldo_atual NUMERIC(14,2) NOT NULL DEFAULT 0; UNIQUE(empresa_id,nome). |
| categoria | id BIGINT identity PK; descricao VARCHAR(100) NOT NULL UNIQUE; natureza CHAR(1) NOT NULL CHECK IN ('R','D'). |
| lancamento | id BIGINT identity PK; conta_id e categoria_id BIGINT FK NOT NULL; data_lancamento DATE NOT NULL; documento VARCHAR(60) NOT NULL; observacao VARCHAR(300) NOT NULL DEFAULT ''; valor NUMERIC(14,2) NOT NULL CHECK > 0; criado_em TIMESTAMPTZ NOT NULL; UNIQUE(conta_id,documento). |
| uf | id PK; sigla CHAR(2) NOT NULL UNIQUE, duas letras maiusculas. |
| cidade | id PK; nome VARCHAR(80) NOT NULL; uf_id FK NOT NULL; UNIQUE(nome,uf_id). |
| bairro | id PK; nome VARCHAR(80) NOT NULL UNIQUE. |
| tipo_logradouro | id PK; nome VARCHAR(30) NOT NULL UNIQUE. |
| logradouro | id PK; nome VARCHAR(120) NOT NULL; tipo_logradouro_id FK NOT NULL; UNIQUE(nome,tipo_logradouro_id). |
| endereco | id PK; cep VARCHAR(8); bairro_id, cidade_id e logradouro_id FKs; todos NOT NULL; UNIQUE(cep,bairro_id,cidade_id,logradouro_id). |
| ddi | id PK; codigo VARCHAR(3) NOT NULL UNIQUE, de um a tres digitos. |
| ddd | id PK; codigo VARCHAR(3) NOT NULL UNIQUE, dois ou tres digitos. |

Cardinalidades: Empresa 1:N Conta, EmailEmpresa e TelefoneEmpresa; Conta 1:N Lancamento; Categoria 1:N Lancamento. Cada filho exige exatamente um pai; um pai pode ainda nao possuir filhos. O sistema representa saldo negativo, sem regra de cheque especial porque o enunciado nao a define.

## T4B - especificacao do tipo

```sql
CREATE TYPE financeiro.endereco_completo AS (
  tipo_logradouro varchar(30), logradouro varchar(120), numero varchar(20),
  complemento varchar(80), bairro varchar(80), cidade varchar(80),
  uf char(2), cep varchar(8)
);
```

Numero e texto para permitir valores como S/N. Complemento e opcional. Os outros sete campos sao exigidos pelos CHECKs de empresa; UF deve ter duas letras maiusculas e CEP oito digitos. O PostgreSQL nao permite colocar NOT NULL diretamente nos campos de CREATE TYPE AS; por isso as restricoes ficam na tabela que usa o tipo. NULL nos campos obrigatorios e explicitamente rejeitado, inclusive no CHECK (que de outra forma aceitaria resultado desconhecido).

O JDBC usa `VALUES (?, ?, ROW(?,?,?,?,?,?,?,?)::financeiro.endereco_completo)` para gravar e `SELECT (endereco).cidade` para consultar. O Java nao monta literais de endereco por concatenacao; os valores digitados sao parametros. O tipo tem estrutura definida no catalogo do PostgreSQL, sem JSON, JSONB ou extensao externa. EmpresaService coordena a transacao do cadastro completo; EmpresaDAO recebe a conexao e executa os SQLs.

## T4C - permissoes

O submenu da opcao 5 seleciona um dos tres logins reais por ID. ID 1: administrador, com SELECT/INSERT/UPDATE/DELETE. ID 2: leitor, com SELECT e tentativa de INSERT negada. ID 3: operador, com SELECT/INSERT e DELETE negado. SegurancaService.executar(id) abre uma conexao autenticada somente como o perfil escolhido, consulta as empresas existentes e tenta as escritas na tabela Empresa. A versao executar() sem argumento existe para a verificacao automatizada dos tres perfis.

A saida identifica o login real e mostra os SQLs com seus valores, os resultados e a mensagem original da excecao do driver, sem substituir o erro por comentarios de permissao. INSERT/UPDATE/DELETE usam RETURNING id,cnpj,razao_social: no ID 1, isso mostra a linha inserida, a mudanca de nome e a linha excluida. O SELECT final retorna zero linhas para ela. No ID 3, a linha permanece apos o DELETE negado e e consultada novamente. O DAO continua executando PreparedStatement; a substituicao dos valores no texto serve somente para exibicao.

Nenhum login de demonstracao e superusuario. Os privilegios continuam definidos em sql/03_seguranca.sql: leitor apenas SELECT; operador SELECT e INSERT em empresa, com USAGE na sequencia e sem UPDATE/DELETE; administrador acessa todos os dados do schema. PUBLIC nao recebe acesso ao schema financeiro. Senhas aleatorias ficam somente na pasta local ignorada.

Os testes verificam a negativa por SQLSTATE 42501. Usam SAVEPOINT para recuperar a transacao apos cada comando negado e ROLLBACK ao final, evitando empresas de teste permanentes. Como sequencias nao sao transacionais, podem ocorrer lacunas nos IDs: isso e esperado e nao significa perda de registros.

## T4D - atomicidade

FinanceiroController conduz a escolha da conta, a leitura do Lancamento pelo console e a confirmacao do usuario. Depois FinanceiroService abre a conexao e desliga autocommit. Chama ContaDAO para bloquear a conta, CategoriaDAO para ler a natureza, LancamentoDAO para inserir e ContaDAO para atualizar o saldo. A categoria de receita soma e a de despesa subtrai. Os quatro DAOs existentes recebem Connection e nao controlam transacoes; os modelos ficam em classes/casodeuso e classes/generico. Detalhes em ESTRUTURA.md.

- OK: commit() confirma INSERT e UPDATE juntos.
- Rollback intencional: rollback() cancela os dois comandos.
- Incompleta: o processo Java auxiliar chama FinanceiroService.iniciarIncompleta para abrir a transacao e executar os dois comandos. Depois chama Runtime.halt(23), sem commit(), rollback() ou close(). O servidor detecta a desconexao e desfaz a transacao. O processo principal continua no console para mostrar que o lancamento nao existe e o saldo foi preservado.

Nao se usa apenas uma excecao com finally para fingir a interrupcao abrupta. Cada cenario confirma os resultados por uma nova conexao JDBC, comparando saldo e quantidade de lancamentos com um documento exclusivo. A opcao 6 informa que COMMIT grava R$ 10,00 de verdade e pede confirmacao; as opcoes 7 e 8 nao alteram o estado persistido. Todas as interacoes sao feitas no console, com opcoes numeradas.

A saida do T4D exibe os SQLs, seus parametros e os resultados reais, sem substituir os resultados por frases de sucesso. INSERT de Lancamento e UPDATE de Conta usam RETURNING. SaldoConta e RegistroLancamento recebem os campos lidos do ResultSet; ResultadoSQL transporta o comando, as colunas, as linhas e o erro original quando houver. FinanceiroService registra os resultados por um Consumer, sem depender do console. TransacaoService reune as etapas e executa os SELECTs finais. As verificacoes de expectativas ficam nos testes; o console mostra o estado efetivamente encontrado.

Na variante abrupta, ProcessoInterrompido envia as etapas coletadas por ObjectOutputStream antes de halt(23), sem fechar a Connection. A JVM principal recebe os resultados do processo, verifica seu codigo real de saida e consulta o banco. Uma falha de SQL no processo auxiliar tambem devolve a excecao original. COMMIT/ROLLBACK normais e perda abrupta da conexao nao exigem que o servidor retorne um erro SQL ao programa principal.

O relatorio verificacao-t4d-sql.txt cobre COMMIT de receita/despesa, interrupcao, ROLLBACK, cancelamento, falha real de UPDATE depois de INSERT e RETURNING de valores alterados pelo servidor. Esses testes usam somente o cluster temporario na porta 5545.
