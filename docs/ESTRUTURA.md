# Estrutura Java

O main continua em `src/main/java/br/edu/bd/Aplicacao.java`. Toda a interacao continua no console Run do IntelliJ, com as mesmas opcoes 0 a 9.

```text
src/main/java/br/edu/bd/
  Aplicacao.java
  classes/
    casodeuso/
      Empresa.java, Conta.java, Categoria.java, Lancamento.java
      Movimento.java, RegistroEmpresa.java, ResultadoPerfil.java, ResultadoTransacao.java
      SaldoConta.java, RegistroLancamento.java
    generico/
      Endereco.java, Email.java, Telefone.java, Opcao.java, ResultadoSQL.java
  controller/
    EmpresaController.java, FinanceiroController.java, SegurancaController.java
  service/
    EmpresaService.java, FinanceiroService.java
    SegurancaService.java, TransacaoService.java
  DAO/
    EmpresaDAO.java, ContaDAO.java, CategoriaDAO.java, LancamentoDAO.java
  console/
    MenuConsole.java, ConsoleEntrada.java
    ConsoleEmpresa.java, ConsoleFinanceiro.java, ConsoleDemonstracoes.java
    ConsoleSeguranca.java
  infra/
    ConnectionFactory.java, Configuracao.java, PrepararBanco.java
  demonstracao/
    DadosDemonstracao.java, ProcessoInterrompido.java

src/test/java/br/edu/bd/
  VerificarProjeto.java
```

## Responsabilidades

- **Aplicacao:** monta os objetos e suas dependencias; inicia o menu. Nao contem SQL.
- **Console:** recebe entradas e apresenta resultados. Transforma data/valor digitados em tipos Java e formata endereco, moeda e relatorios. Nao conhece JDBC, Services ou DAOs.
- **Controller:** conduz o fluxo de interacao. Solicita listas ao servico, pede escolhas/dados ao console, pede confirmacao e chama a operacao apropriada. Nao executa SQL nem decide COMMIT/ROLLBACK.
- **Service:** valida a operacao, aplica regras e controla a transacao. Trabalha com os modelos; nao pergunta nem imprime. Repassa a mesma Connection aos DAOs participantes.
- **DAO:** executa SQL, preenche modelos e fecha somente seus Statements/ResultSets. Nao abre/fecha Connection, nao controla autocommit e nao faz commit/rollback.
- **Classes:** representam os dados, independentes de persistencia e apresentacao. Os records foram mantidos como classes imutaveis, agora em arquivos proprios. Endereco continua mapeando os oito campos do composto existente; as oito tabelas adicionais sao criadas pelo SQL de preparacao.
- **Infra:** endereco do servidor, credenciais locais e criacao de conexoes. PrepararBanco continua sendo uma ferramenta inicial por console.
- **Demonstracao:** dados ficticios de teste e a JVM auxiliar que termina abruptamente no T4D.

## Exemplo: opcao 2

1. MenuConsole encaminha a opcao a FinanceiroController.registrar.
2. O Controller solicita empresas/contas/categorias ao FinanceiroService e pede os dados ao ConsoleFinanceiro.
3. O console devolve um objeto Lancamento e recebe a confirmacao do usuario.
4. O Controller chama FinanceiroService.registrar somente depois da confirmacao.
5. O Service abre a conexao e a transacao, bloqueia a conta via ContaDAO, consulta a natureza via CategoriaDAO, executa LancamentoDAO.inserir e ContaDAO.atualizarSaldo.
6. O Service confirma tudo com COMMIT ou desfaz tudo com ROLLBACK. Os DAOs nunca confirmam isoladamente.
7. O Controller solicita ao console que mostre o novo saldo.

## DAOs necessarios

EmpresaDAO cobre o cadastro da empresa com seus contatos. ContaDAO fornece conta/saldo, bloqueio e UPDATE. CategoriaDAO e necessario para a selecao e a natureza da receita/despesa. LancamentoDAO realiza INSERT e consultas de extrato/documento.

Nao ha DAO de endereco porque ele e um valor composto de Empresa. Nao ha DAOs separados de e-mail/telefone porque nao existem fluxos independentes de manutencao desses contatos. As operacoes UPDATE/DELETE em EmpresaDAO sao usadas nos testes reais de permissoes do T4C; nao foi criado CRUD de manutencao sem uso.

Na adicao das tabelas do MER, o INSERT original de Empresa continua gravando o mesmo valor endereco_completo. Os triggers de sql/06_tabelas_mer.sql vinculam/cadastram Endereco e os codigos telefonicos dentro dessa transacao, sem COMMIT/ROLLBACK. Por isso nao foram acrescentados DAOs ou menus de manutencao. O T4C usa um endereco ja existente, evitando escritas auxiliares no teste do operador. Relatorios: verificacao-tabelas-mer.txt e verificacao-mer-banco-real.txt.

## Preservacao e verificacao

Banco, tabelas, tipo composto, carga, regras e opcoes de menu foram preservados. Os SQLs foram comparados byte a byte com a copia anterior. A versao anterior e a refatorada tambem foram executadas na mesma base temporaria para comparar as saidas do console; UUIDs de demonstracao foram normalizados nessa comparacao.

Relatorios: `verificacao-estrutura.txt`, `verificacao-console.txt`, `verificacao-services.txt`, `verificacao-t4c-login.txt` e `verificacao-t4d-sql.txt`. Os testes de escrita e falhas forcadas usam apenas a base temporaria na porta 5545. O banco de apresentacao na porta 6666 nao e recriado nem reinicializado pela refatoracao.

Na evolucao do T4C, ConsoleSeguranca exibe os IDs de login e SegurancaController conduz a escolha e repeticao. SegurancaService executa o perfil escolhido e devolve ResultadoPerfil com o login real e uma lista de ExecucaoSQL: comando, parametros, linhas retornadas ou erro original com SQLSTATE. RegistroEmpresa guarda id, CNPJ e razao social lidos do ResultSet. ConsoleDemonstracoes formata os SQLs e seus resultados, sem substituir erros por mensagens de permissao. RETURNING mostra as modificacoes reais do ID 1. Os erros SQLSTATE 42501 vem das tentativas reais de INSERT/DELETE. Nao foi adicionado DAO nem alterado o SQL de privilegios.

No T4D, FinanceiroService continua dono da transacao e registra as etapas como ResultadoSQL. Os DAOs existentes devolvem SaldoConta e RegistroLancamento lidos de SELECT/RETURNING. TransacaoService agrega as etapas e consulta o resultado por outra conexao; ResultadoTransacao leva essa lista ao console. ProcessoInterrompido transmite as mesmas etapas antes de halt(23), sem formatar mensagens ou fechar a Connection no caminho de sucesso. ConsoleDemonstracoes apenas imprime os comandos, as tabelas e a excecao original. Os DAOs continuam sendo somente quatro.
