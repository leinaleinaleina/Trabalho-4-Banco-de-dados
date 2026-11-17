# Compartilhar pelo GitHub e IntelliJ

Repositorio: https://github.com/leinaleinaleina/Trabalho-4-Banco-de-dados

O projeto Maven fica na raiz do repositorio. A pasta MER ja existente foi preservada. Seus arquivos Workbench servem de referencia; o banco PostgreSQL e recriado pelos scripts SQL e pelo PrepararBanco, conforme COMO_RODAR.md.

## Commit e push sao etapas diferentes

- **Commit** registra uma versao no Git do seu computador.
- **Push** envia os commits ao GitHub. Seu colega so consegue baixar essa versao depois do push.
- **Pull** traz para seu computador os commits publicados por outra pessoa.

## Enviar alteracoes pelo IntelliJ

1. Abra esta pasta pelo pom.xml. O Git deve reconhecer a raiz e a branch main.
2. Abra a janela Commit ou pressione **Ctrl+K**.
3. Confira os arquivos selecionados. Codigo, SQLs, documentacao e PDF de entrega devem aparecer. .local, .idea, target, out, logs, dumps e senhas devem ficar fora.
4. Escreva uma mensagem que descreva a alteracao e escolha **Commit**.
5. Abra **Git > Push** ou pressione **Ctrl+Shift+K**. Confira o destino **origin/main** e confirme **Push** para publicar.
6. Caso o IntelliJ solicite login no GitHub, entre com sua propria conta, que precisa ter permissao de escrita nesse repositorio. Nao salve tokens ou senhas em arquivos do projeto.

Se o IntelliJ pedir o endereco remoto, use **Git > Manage Remotes** e confira origin com a URL HTTPS do repositorio. Nao use a funcao Share Project on GitHub para criar outro repositorio com o mesmo nome.

## Trabalhar com o colega

1. O colega clona o repositorio e segue docs/COMO_RODAR.md.
2. Cada pessoa usa suas proprias credenciais em .local e seu proprio PostgreSQL.
3. Antes de comecar novas alteracoes, use **Git > Pull** para receber o trabalho publicado pelo outro.
4. Depois de alterar, testar e conferir os arquivos, faca Commit e Push.
5. Se houver conflito, confira as duas alteracoes. Nao use force push para resolver um conflito comum.

## O que vai junto

- pom.xml e src/: codigo e declaracao do driver JDBC.
- sql/: estrutura das 14 tabelas, tipo composto, carga inicial e perfis.
- conexao.properties: sugestoes publicas de conexao, sem senhas.
- .run/: configuracoes portaveis de execucao.
- docs/, tools/ e output/pdf/Trabalho_T4.pdf: documentacao e entrega.
- MER/: material que ja estava no repositorio.

O arquivo MER/TB4.mwb.bak ja estava versionado no remoto. Ele foi preservado com o restante da pasta MER; a regra *.bak evita adicionar novos backups. O .gitignore nao retira arquivos que ja fazem parte do historico.

O conteudo do seu PostgreSQL nao e enviado automaticamente por um commit. Neste projeto foi escolhida a carga inicial compartilhada em sql/02_dados.sql, nao uma exportacao dos dados atuais da maquina de desenvolvimento.

Referencias: [Commit e push no IntelliJ](https://www.jetbrains.com/help/idea/commit-and-push-changes.html) e [arquivos ignorados no GitHub](https://docs.github.com/en/get-started/git-basics/ignoring-files).
