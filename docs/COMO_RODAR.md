# Rodar o projeto em outro computador

O repositorio leva o codigo, a estrutura das 14 tabelas e os dados iniciais de exemplo. Ele nao leva o PostgreSQL instalado, as senhas, os arquivos internos do servidor ou os lancamentos feitos depois da carga inicial no computador de outra pessoa.

## Requisitos

- IntelliJ IDEA com JDK 17 ou superior selecionado no projeto.
- PostgreSQL instalado e em execucao. A versao de referencia testada e PostgreSQL 18.
- Usuario administrador do PostgreSQL e sua senha, definidos na instalacao desse computador.
- Internet na primeira importacao para o Maven baixar o driver JDBC e os componentes de compilacao.

Nao e necessario instalar MySQL, copiar um JAR manualmente ou usar PowerShell. O pgAdmin e opcional para inspecionar o banco.

## 1. Baixar e abrir

1. No IntelliJ, use **Get from VCS** (ou **Clone Repository**) e informe `https://github.com/leinaleinaleina/Trabalho-4-Banco-de-dados.git`.
2. Abra o `pom.xml` da raiz como projeto Maven.
3. Selecione JDK 17 ou superior e aguarde a importacao das dependencias.
4. Se o projeto foi baixado por ZIP, extraia-o e abra o mesmo `pom.xml`. Nao execute os arquivos Java como um projeto separado do Maven.

## 2. Preparar o seu banco uma vez

1. Execute `src/main/java/br/edu/bd/infra/PrepararBanco.java` pela seta verde do `main`.
2. No console Run, informe o servidor, a porta, o usuario administrador e a senha do PostgreSQL desse computador.
3. Para uma instalacao local comum, os valores sugeridos sao `127.0.0.1`, porta `5432` e usuario `postgres`. Use outra porta se sua instalacao usa outro valor.
4. Aguarde `Banco preparado. Execute Aplicacao.java...`.

As configuracoes e as senhas geradas ficam na pasta `.local`, excluida do Git. Cada colega tem os seus arquivos locais. A senha administrativa digitada nao e salva pelo programa; as credenciais dos tres perfis do trabalho sao geradas e salvas localmente.

O projeto usa o banco **bd_t4_individual**, schema **financeiro**. A preparacao cria o tipo **endereco_completo**, as 14 tabelas, seus relacionamentos, os tres logins e a carga inicial: **duas empresas, quatro contas e 20 lancamentos**. Cada conta parte de R$ 1.150,00 antes dos novos cadastros e testes.

O arquivo publico `conexao.properties` fornece apenas sugestoes. A porta 6666 utilizada na maquina de desenvolvimento nao e uma exigencia; o endereco escolhido aqui e salvo em `.local/conexao.properties`.

## 3. Usar o sistema

Execute `src/main/java/br/edu/bd/Aplicacao.java` e interaja pelo console Run:

| Opcao | Funcao |
|---|---|
| 1 / 9 | Consultar um extrato / todos os extratos |
| 2 | Registrar receita ou despesa |
| 3 / 4 | Cadastrar / consultar empresa com endereco composto |
| 5 | Demonstrar os tres perfis reais do T4C |
| 6 / 7 / 8 | COMMIT / interrupcao abrupta / ROLLBACK |
| 0 | Sair |

A opcao 6 confirma um lancamento real de R$ 10,00. As opcoes 7 e 8 preservam o estado persistido. As demonstracoes T4C terminam com ROLLBACK. O roteiro detalhado esta em `docs/ROTEIRO.md`.

Ao reiniciar o computador, basta iniciar o PostgreSQL caso necessario e executar Aplicacao.java. Nao e necessario preparar novamente a cada execucao.

## Quais SQLs recriam o banco

PrepararBanco executa os scripts na ordem correta. Nao e necessario importa-los manualmente no pgAdmin:

| Arquivo | Conteudo |
|---|---|
| sql/00_criar_banco.sql | Criacao do banco; o Java verifica e cria automaticamente, fora da transacao de estrutura |
| sql/01_estrutura.sql | Tipo composto original e seis tabelas principais |
| sql/06_tabelas_mer.sql | Oito tabelas adicionais, FKs, triggers e vinculacao dos dados |
| sql/02_dados.sql | Carga inicial ficticia; aplicada somente quando nao ha empresas |
| sql/03_seguranca.sql | Logins, GRANT e REVOKE; senhas definidas localmente pela preparacao |
| sql/04_consultas.sql | Exemplos de consultas; nao e parte da carga |
| sql/05_transacoes.sql | Exemplo didatico de transacao; nao executar como carga inicial |

Arquivos SQL nao devem ser executados simplesmente na ordem alfabetica. Para instalacao manual, crie o banco com 00 fora de transacao; no banco criado, use 01, 06, 02 e 03. Os scripts 04 e 05 sao exemplos de uso. A criacao manual tambem precisa do comentario identificador do banco e de credenciais locais para ser reconhecida pelo PrepararBanco; prefira a preparacao Java.

## Se aparecer erro

- **No suitable driver / dependencia vermelha:** abra o pom.xml como Maven, confira a conexao com a internet e recarregue o projeto na janela Maven. O driver PostgreSQL ja esta declarado no pom.xml.
- **Connection refused:** confira se o servico PostgreSQL esta em execucao e se a porta informada esta correta.
- **Falha de senha:** informe a senha do seu proprio PostgreSQL ao executar PrepararBanco. Nao use a senha de outro colega.
- **Banco ou login ja existe sem pertencer a esta preparacao:** o programa preserva o existente. Nao copie a pasta .local de outro computador para contornar isso; confira no pgAdmin a origem desse banco/login antes de prosseguir.

## Arquivos compartilhados

Versionar codigo, pom.xml, SQLs, documentacao, .gitignore, .gitattributes, conexao.properties sem senha, configuracoes portaveis .run e o PDF de entrega.

Nao compartilhar .local, .idea, target, out, backups, dumps, senhas ou arquivos internos do PostgreSQL. O .gitignore filtra arquivos novos; se um arquivo ja tiver sido commitado anteriormente, ele precisa ser removido do acompanhamento do Git, preservando sua copia local.

O repositorio e a base de codigo compartilhada; cada colega utiliza seu proprio servidor e seu proprio banco local. Um cadastro feito em um computador nao aparece automaticamente no outro. Para receber alteracoes de codigo, use Git > Pull e siga as instrucoes da alteracao caso haja atualizacao de estrutura.
