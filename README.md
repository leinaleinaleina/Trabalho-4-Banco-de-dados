# Trabalho T4 - Controle de Despesas e Receitas

**Moises S e Thaynara Nascimento de Jesus**

Projeto para T4A, T4B, T4C e T4D, em Java 17+, JDBC e PostgreSQL, com **menu numerado no console**. O endereco usa um **tipo composto nativo**, `financeiro.endereco_completo`. Nao usa JSON nem MySQL.

**Para o colega que acabou de clonar:** siga [docs/COMO_RODAR.md](docs/COMO_RODAR.md). O repositorio recria as 14 tabelas com a carga inicial; cada computador informa sua propria conexao e gera suas credenciais locais. Os cadastros posteriores de outro computador nao sao sincronizados pelo Git.

Para enviar e receber codigo pelo IntelliJ, consulte [docs/COMO_PUBLICAR.md](docs/COMO_PUBLICAR.md).

## Abrir pelo IntelliJ

1. Escolha **File > Open** e abra o `pom.xml` desta pasta como projeto Maven.
2. Selecione um JDK 17 ou superior e aguarde a importacao do Maven.
3. Abra `src/main/java/br/edu/bd/Aplicacao.java` e clique na seta verde do `main`.
4. Clique dentro do console **Run**, digite a opcao do menu e pressione Enter. Todas as interacoes, resultados e erros aparecem ali.
5. Se o banco ainda nao estiver preparado, o proprio console pede servidor, porta, usuario administrador e senha do PostgreSQL. A preparacao cria **somente** o banco `bd_t4_individual`, suas tabelas e os logins do trabalho.
6. Depois da preparacao, o programa usa o login proprio `bdt4_ind_admin`, com credencial gerada localmente. Nao pede a senha administrativa novamente.

Nesta maquina o servidor existente usa **127.0.0.1:6666**, usuario administrativo `postgres`. Essa porta foi gravada somente em `.local/conexao.properties`. Em outra maquina o console sugere o padrao 5432 de `conexao.properties`; ajuste conforme a instalacao. Nao e preciso instalar outro PostgreSQL nem executar comandos no PowerShell.

Para refazer a configuracao, execute `infra/PrepararBanco.java` (classe `br.edu.bd.infra.PrepararBanco`). Repetir preserva registros existentes; nao apaga tabelas nem reaplica a carga quando ja existem empresas. Se o nome do banco pertencer a outro projeto, a preparacao recusa modifica-lo. `IF NOT EXISTS` nao aplica futuras mudancas de estrutura: alteracoes posteriores exigem migracao SQL.

## Menu do console

| Opcao | O que demonstrar |
|---|---|
| 1 - T4A: consultar extrato | Escolher empresa e conta; consultar data, categoria, natureza, documento, valor, saldo acumulado e saldo atual. Duas consultas SQL montam cada extrato. |
| 2 - T4A: registrar lancamento | Digitar data, categoria, documento, observacao e valor. INSERT em lancamento e UPDATE em conta na mesma transacao. |
| 3 - T4B: cadastrar empresa | Digitar empresa, endereco e contatos opcionais. INSERT do composto nativo e conferencia imediata por SELECT. |
| 4 - T4B: consultar empresas | SELECT das empresas, campos do endereco composto e contatos. |
| 5 - T4C: login por perfil | Escolher ID 1, 2 ou 3; consultar empresas existentes e demonstrar somente as permissoes daquele login real. |
| 6 - T4D: COMMIT | SQLs, INSERT/UPDATE com RETURNING, COMMIT e SELECTs por outra conexao mostrando o lancamento e o novo saldo. |
| 7 - T4D: interrupcao abrupta | SQLs executados pela JVM auxiliar, codigo real de saida 23 e SELECTs mostrando saldo preservado e zero lancamentos para o documento. |
| 8 - T4D: ROLLBACK | SQLs, resultados de INSERT/UPDATE dentro da transacao, ROLLBACK e SELECTs confirmando que nada foi persistido. |
| 9 - T4A: todos os extratos | Mostra todas as contas em sequencia, agilizando a demonstracao das duas empresas. |
| 0 - Sair | Encerra o programa. |

Nas listas, Enter escolhe o item 1; digite 0 para voltar. Em qualquer campo, `:voltar` cancela antes de gravar. Datas sugerem o dia atual; complemento, observacoes e contatos podem ficar vazios. Entradas invalidas sao pedidas novamente. Um cadastro com campos invalidos no banco e desfeito por ROLLBACK.

Os valores de lancamento sao positivos: a natureza R/D da categoria determina o sinal no saldo. Informe `120,50` ou `120.50`, sem separador de milhar. Documento deve ser unico dentro da conta. Datas invalidas sao rejeitadas. O saldo pode ficar negativo, pois o enunciado nao proibe isso.

## Carga inicial

Duas empresas ficticias, duas contas por empresa (Caixa e Conta bancaria), seis categorias, um e-mail e um telefone por empresa, cinco lancamentos por conta. Sao **20 lancamentos**, cada conta iniciando com saldo de **R$ 1.150,00**. Testes ou lancamentos posteriores podem aumentar essa quantidade e mudar os saldos.

Cadastro de empresa e lancamento aceitam dados digitados. Os valores iniciais existem para atender a demonstracao minima; as consultas sempre leem o banco. Cadastros de categorias e novas contas sao feitos por SQL; nao ha menus adicionais de manutencao que o trabalho nao pede.

## O que o enunciado exige de INSERT

No caso 1, registrar uma receita/despesa exige INSERT em Lancamento e UPDATE do saldo da Conta. T4B(b) exige que o programa Java/JDBC realize INSERT e SELECT na tabela com dado complexo; nao fixa uma quantidade minima de empresas novas para essa demonstracao. T4C exige mostrar um login que pode inserir e nao pode excluir, e os outros perfis. T4D exige pelo menos duas modificacoes em tabelas diferentes, envolvendo INSERT e UPDATE, nos tres cenarios.

O minimo de duas empresas, duas contas por empresa e cinco lancamentos por conta e uma exigencia dos dados a consultar: pelo menos 20 lancamentos no total. O texto nao exige digitar esses 20 durante a apresentacao. A carga inicial os fornece; as opcoes 2 e 3 demonstram novos INSERTs feitos pelo Java. Os prototipos de telas do enunciado sao representados pelos mesmos campos e consultas no console.

## Banco no pgAdmin

Use o **pgAdmin**, pois o MySQL Workbench nao administra PostgreSQL.

1. Abra ou registre um servidor com host `127.0.0.1`, porta `6666` nesta maquina, banco de manutencao `postgres` e usuario `postgres`.
2. Informe a senha do seu PostgreSQL.
3. Atualize **Databases > bd_t4_individual > Schemas > financeiro > Tables**.
4. Em **Types**, veja `endereco_completo`.
5. Abra o Query Tool no banco `bd_t4_individual` e execute as consultas de `sql/04_consultas.sql`.

Servidor, banco e schema sao coisas diferentes: o servidor existente escuta na porta 6666; o banco do trabalho e `bd_t4_individual`; dentro dele as 14 tabelas ficam no schema `financeiro`.

## Organizacao e requisitos

- `sql/00_criar_banco.sql`: criacao do banco, fora de transacao. A preparacao Java verifica se ele existe.
- `sql/01_estrutura.sql`: seis tabelas, chaves, CHECKs e tipo composto (T4A e T4B a).
- `sql/06_tabelas_mer.sql`: acrescenta bairro, UF, tipo_logradouro, DDD, DDI, cidade, logradouro e endereco, com as ligacoes do MER. Preserva nomes das seis tabelas anteriores e o tipo endereco_completo de oito campos.
- `sql/02_dados.sql`: carga minima, aplicada uma unica vez.
- `sql/03_seguranca.sql`: tres logins, GRANT e REVOKE (T4C). Senhas reais nao estao neste arquivo.
- `sql/04_consultas.sql`: consultas das telas e do composto.
- `sql/05_transacoes.sql`: exemplo didatico de INSERT e UPDATE atomicos (T4D).
- `docs/modelo-er.mmd` e `docs/modelo-er.svg`: modelo unico editavel e versao visual.
- `docs/ESPECIFICACAO.md`: regras, normalizacao e especificacao dos atributos.

Ao atualizar um banco existente, execute PrepararBanco pelo IntelliJ e informe a senha do dono das tabelas (postgres nesta instalacao). O programa aplica 01 e 06 na mesma transacao, preserva os registros, aplica a carga somente se nao ha empresas e atualiza as permissoes. Para instalacao manual, a ordem e 00 (fora de transacao), 01, 06, 02 (somente banco vazio), 03. Nao execute 02 novamente em um banco ja usado.

### Correspondencia com o MER

As oito tabelas adicionais sao fisicas e possuem dados e chaves estrangeiras. A Empresa continua contendo `endereco financeiro.endereco_completo`, com os mesmos oito campos. O novo `endereco_id` a relaciona ao cadastro Endereco do MER. Triggers criam/reutilizam os cadastros a partir do valor composto, na transacao do cadastro. DDI e DDD sao tabelas separadas; os campos de telefone referenciam seus codigos UNIQUE, preservando os INSERTs e SELECTs anteriores. Os triggers usam os privilegios do login que executa o comando (SECURITY INVOKER).

Os nomes existentes foram preservados: Conta_Bancaria do MER corresponde a conta; TipoTransacao a categoria; Transacao a lancamento; FoneEmpresa a telefone_empresa; EmailEmpresa a email_empresa. O diagrama atualizado e o SQL mostram os nomes fisicos. Nao foram criadas tabelas paralelas para essas cinco entidades.
- `docs/ROTEIRO.md`: sequencia de apresentacao.
- `docs/verificacao.txt`: resultado dos testes de persistencia feitos na instalacao de referencia.
- `docs/verificacao-console.txt`: testes do menu, entradas digitadas e confirmacao dos dados no banco temporario.
- `docs/verificacao-services.txt`: validacao da separacao Service/DAO e de falhas apos INSERTs bem-sucedidos.
- `docs/verificacao-estrutura.txt`: limites das camadas, SQL preservado e comparacao com a versao anterior.
- `docs/ESTRUTURA.md`: mapa de pacotes e exemplo do fluxo Controller -> Service -> DAO.
- `output/pdf/Trabalho_T4.pdf`: material de entrega com modelo, especificacao e SQL de seguranca.

### Organizacao Java

| Pacote / classe | Responsabilidade |
|---|---|
| Aplicacao | Mantem o main existente; monta as dependencias e inicia o menu. |
| classes/casodeuso | Empresa, Conta, Categoria, Lancamento, Movimento e resultados das demonstracoes. |
| classes/generico | Endereco, Email, Telefone, Opcao e ResultadoSQL, sem dependencia de JDBC ou console. |
| console | MenuConsole, ConsoleEntrada e formatacao de empresas, extratos e demonstracoes. |
| controller | EmpresaController, FinanceiroController e SegurancaController: conduzem escolhas, confirmacoes e exibicao. |
| service | EmpresaService e FinanceiroService: regras e transacoes. SegurancaService e TransacaoService: cenarios T4C e T4D. |
| DAO | Somente EmpresaDAO, ContaDAO, CategoriaDAO e LancamentoDAO, usados pelas operacoes atuais. |
| infra | ConnectionFactory, Configuracao e PrepararBanco: conexoes, configuracao e preparacao inicial. |
| demonstracao | DadosDemonstracao e ProcessoInterrompido, necessarios para os exemplos T4C/T4D. |
| src/test/java/.../VerificarProjeto | Verificacao de integracao, separada do codigo da aplicacao. A variante COMMIT adiciona R$ 10,00; execute em dados de demonstracao. |

### Camadas e transacoes

Fluxo: `MenuConsole -> Controller -> Service -> DAO -> PostgreSQL`.

O Controller conduz a sequencia: escolher empresa/conta, solicitar os dados ao console, pedir confirmacao e chamar o Service. A confirmacao do usuario acontece antes de abrir a transacao. O console le e formata os valores; nao acessa servicos ou DAOs. Os Services nao conhecem o console e devolvem objetos, inclusive nos resultados de seguranca e transacao.

O Service abre a conexao, desliga o autocommit, coordena os comandos SQL e decide confirmar ou desfazer a operacao inteira. O DAO recebe essa mesma Connection, executa SQL e fecha apenas seus PreparedStatements e ResultSets. Nao abre/fecha a Connection, nao chama commit/rollback e nao altera autocommit.

Em `FinanceiroService.registrar`, INSERT do lancamento e UPDATE do saldo sao confirmados juntos. `registrarComRollback` e a variante explicita do T4D(c). A regra de somar receita ou subtrair despesa tambem fica no Service. `EmpresaService.cadastrar` confirma empresa e contatos juntos; se um telefone falhar, ate a empresa e os e-mails ja inseridos sao desfeitos.

EmpresaDAO grava a empresa e seus contatos como um cadastro unico; por isso nao ha EmailDAO, TelefoneDAO ou EnderecoDAO. O valor composto original continua na Empresa; os cadastros auxiliares do MER sao vinculados por triggers da mesma transacao. ContaDAO consulta/bloqueia a conta e atualiza o saldo; CategoriaDAO consulta as categorias; LancamentoDAO insere e consulta os lancamentos. Permanecem somente quatro DAOs. As consultas devolvem modelos tipados, sem List<String[]> ou modelos definidos dentro do DAO.

Para T4D(b), `FinanceiroService.iniciarIncompleta` e uma excecao deliberada de demonstracao: executa os comandos numa transacao aberta e devolve a Connection ao processo auxiliar, que chama Runtime.halt(23) sem fecha-la nem confirmar/desfazer. A aplicacao normal nao usa esse metodo. PrepararBanco e as rotinas de verificacao controlam transacoes tecnicas de preparacao/teste, fora dos DAOs.

Nas opcoes 6, 7 e 8, o console apresenta os comandos SQL, as linhas retornadas por RETURNING e os SELECTs finais por outra conexao. Se um comando falhar, sua excecao original aparece junto do SQL; o servico desfaz a transacao. COMMIT e ROLLBACK intencional nao sao erros: quando funcionam, aparecem os comandos e seus resultados. A interrupcao abrupta mostra o codigo real do processo, sem inventar um erro SQL. Os resultados dentro da transacao podem mostrar uma linha e um saldo alterado, enquanto os SELECTs apos ROLLBACK/interrupcao mostram zero linhas e o saldo anterior.

Os comandos continuam parametrizados por PreparedStatement. O processo auxiliar transmite os resultados reais dos comandos para a JVM principal, que os formata no console; a transferencia e feita antes de halt(23) e nao fecha a conexao do banco. Relatorio: `docs/verificacao-t4d-sql.txt`.

## Seguranca e arquivos compartilhados

Os logins do T4C sao `bdt4_ind_admin`, `bdt4_ind_leitor` e `bdt4_ind_operador`. Todos sao NOSUPERUSER, NOCREATEDB e NOCREATEROLE. Administrador tem acesso a todos os dados do projeto; leitor so consulta; operador consulta e insere em empresa, mas nao altera nem exclui. O perfil operador foi limitado a empresa para demonstrar a exigencia em uma tabela.

Na opcao **5 - Login por perfil**, escolha:

| ID | Login real | Demonstracao executada |
|---|---|---|
| 1 | bdt4_ind_admin | SELECT das empresas existentes; INSERT, UPDATE e DELETE com RETURNING mostram a linha inserida, o nome alterado e a linha excluida. Um SELECT final retorna zero linhas para esse ID. |
| 2 | bdt4_ind_leitor | SELECT das empresas existentes e tentativa de INSERT, seguida da mensagem original da excecao do PostgreSQL. |
| 3 | bdt4_ind_operador | SELECT e INSERT com o registro retornado; DELETE exibe a mensagem original de erro, e outro SELECT mostra que a linha continua presente. |

Depois do resultado, o menu de IDs reaparece. Escolha outro perfil ou **0** para voltar ao menu principal. IDs sao atalhos da aplicacao para logins distintos, nao IDs de empresas. O programa autentica uma nova conexao com o usuario/senha daquele perfil; nao simula permissoes com mensagens predefinidas ou com a conexao administrativa.

O console mostra os SQLs com seus valores, os resultados devolvidos pelo banco e a mensagem original do driver quando ocorre erro. A execucao continua parametrizada por PreparedStatement; os valores sao incluidos no texto somente para exibicao. Nao ha mensagens de status como "operacao permitida" ou "operacao negada". O SELECT inicial mostra ate dez empresas existentes e a quantidade de linhas desse resultado.

As tentativas de escrita usam uma empresa temporaria valida e terminam com ROLLBACK, preservando os cadastros existentes. Um SAVEPOINT permite recuperar a transacao apos o erro original e concluir a demonstracao. A transacao termina antes de perguntar o proximo ID. O relatorio dos testes esta em `docs/verificacao-t4c-login.txt`.

As senhas desses tres logins sao aleatorias e ficam **em texto no arquivo local** `.local/perfis.properties`, ignorado pelo Git. Nao sao a senha do administrador do PostgreSQL. Essa e uma solucao local de demonstracao, nao um cofre de senhas. A senha administrativa digitada nao e salva. Para entrar no pgAdmin como um dos perfis, consulte esse arquivo apenas no seu computador; nao o envie a ninguem.

Na preparacao inicial pelo console Run do IntelliJ, a senha digitada pode ficar visivel no historico do console. Ela nao e impressa pelo programa nem salva em arquivo. Uma alternativa e informar `T4_ADMIN_PASSWORD` apenas no ambiente local da execucao de PrepararBanco, sem commitar essa configuracao. Em um terminal com System.console disponivel, readPassword oculta a entrada. A apresentacao normal usa os perfis ja preparados e nao solicita a senha administrativa.

Commitar `pom.xml`, `README.md`, `.gitignore`, `conexao.properties`, `src/`, `sql/`, `docs/`, `output/pdf/`, `.run/` e `tools/`. Nao enviar `.local/`, `.idea/`, `target/`, `*.iml` ou senhas. Se enviar por ZIP ou upload manual, o `.gitignore` nao filtra a selecao: exclua esses itens manualmente.

Este projeto e independente da pasta anterior `T4B/` e do `TB4.mwb`. O novo modelo e feito para PostgreSQL; o arquivo Workbench antigo e apenas referencia historica e nao e a modelagem final desta versao.

## Verificacao e limites

O projeto foi compilado com JDK 21, alvo Java 17. A verificacao usa conexoes JDBC reais, testa carga minima, saldo, unicidade de documento, CHECK do composto, logins e os tres cenarios de transacao. O ambiente temporario de testes usa uma porta separada e e desligado ao final; nao e o servidor usado na apresentacao.

Nao existe trigger oculta atualizando saldo: `FinanceiroService.modificar` coordena `LancamentoDAO.inserir` e `ContaDAO.atualizarSaldo` na mesma Connection. Por isso comandos SQL manuais que modifiquem lancamentos precisam manter o saldo na mesma transacao. Edicoes diretas de categorias ou valores historicos tambem exigem tratamento de saldo. Os menus do projeto seguem essa regra e nao oferecem alteracao/exclusao de lancamentos historicos.

Para reproduzir a verificacao pelo IntelliJ, execute `src/test/java/br/edu/bd/VerificarProjeto.java`. Isso pressupoe a preparacao concluida e acrescenta o lancamento COMMIT de demonstracao. As comparacoes de regressao confirmaram o mesmo resultado no console para consultas, contatos, permissoes, cancelamentos e transacoes nao confirmadas. Nenhum arquivo SQL foi alterado nesta reorganizacao. O gerador de PDF e opcional e nao participa do sistema Java.

Referencias: [tipos compostos PostgreSQL](https://www.postgresql.org/docs/current/rowtypes.html), [GRANT](https://www.postgresql.org/docs/current/sql-grant.html), [transacoes](https://www.postgresql.org/docs/current/tutorial-transactions.html), [pgJDBC](https://jdbc.postgresql.org/download/).
