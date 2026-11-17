package br.edu.bd.service;

import br.edu.bd.DAO.*;
import br.edu.bd.classes.casodeuso.*;
import br.edu.bd.classes.casodeuso.ResultadoTransacao.Modo;
import br.edu.bd.classes.generico.ResultadoSQL;
import br.edu.bd.demonstracao.ProcessoInterrompido;
import br.edu.bd.infra.ConnectionFactory;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** T4D: executa a transacao e consulta o estado persistido por outra conexao. */
public final class TransacaoService {
    private final ConnectionFactory conexoes;
    private final FinanceiroService financeiro;
    private final ContaDAO contas=new ContaDAO();
    private final LancamentoDAO lancamentos=new LancamentoDAO();
    private final CategoriaDAO categorias=new CategoriaDAO();
    public TransacaoService(ConnectionFactory conexoes,FinanceiroService financeiro){this.conexoes=conexoes;this.financeiro=financeiro;}

    public ResultadoTransacao executar(Modo modo,long conta,long categoria)throws Exception {
        List<ResultadoSQL> execucoes=new ArrayList<>();
        Consumer<ResultadoSQL> registro=execucoes::add;
        BigDecimal antes;
        String natureza;
        try(Connection c=conexoes.abrir()) {
            antes=FinanceiroService.executarSQL(registro,ContaDAO.SALDO,List.of(conta),List.of("id","saldo_atual"),
                ()->contas.consultarSaldo(c,conta),FinanceiroService::linhaSaldo).saldo();
            natureza=FinanceiroService.executarSQL(registro,CategoriaDAO.NATUREZA,List.of(categoria),List.of("natureza"),
                ()->categorias.natureza(c,categoria),n->List.of(List.of(n)));
        }
        String documento="T4D-"+UUID.randomUUID();
        Lancamento lancamento=new Lancamento(conta,categoria,LocalDate.now(),documento,"Demonstracao T4D",new BigDecimal("10.00"));
        Integer codigoProcesso=null;
        boolean concluida=true;
        try {
            if(modo==Modo.ABRUPTA) {
                codigoProcesso=interromper(conta,categoria,documento,registro);
                concluida=codigoProcesso==23&&execucoes.stream().noneMatch(r->r.erroOriginal()!=null);
            } else financeiro.executarTransacao(lancamento,modo==Modo.COMMIT,registro);
        }catch(SQLException e){
            concluida=false;
            // Os erros das operacoes ja acompanham o SQL que falhou.
            if(execucoes.stream().noneMatch(r->r.erroOriginal()!=null))
                registro.accept(ResultadoSQL.erro(null,List.of(),e.toString(),e.getSQLState()));
        }
        registro.accept(ResultadoSQL.comando("-- Nova conexao: estado persistido"));
        BigDecimal depois;
        List<RegistroLancamento> registros;
        try(Connection c=conexoes.abrir()) {
            depois=FinanceiroService.executarSQL(registro,ContaDAO.SALDO,List.of(conta),List.of("id","saldo_atual"),
                ()->contas.consultarSaldo(c,conta),FinanceiroService::linhaSaldo).saldo();
            registros=FinanceiroService.executarSQL(registro,LancamentoDAO.POR_DOCUMENTO,List.of(conta,documento),FinanceiroService.COLUNAS_LANCAMENTO,
                ()->lancamentos.consultarDocumento(c,conta,documento),rs->rs.stream().map(FinanceiroService::linhaLancamento).toList());
        }
        return new ResultadoTransacao(modo,conta,documento,natureza,antes,depois,registros.size(),List.copyOf(execucoes),codigoProcesso,concluida);
    }
    private int interromper(long conta,long categoria,String documento,Consumer<ResultadoSQL> registro)throws Exception {
        boolean windows=System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        String executavel=Path.of(System.getProperty("java.home"),"bin",windows?"java.exe":"java").toString();
        String classpath=Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
            .map(p->Path.of(p).toAbsolutePath().normalize().toString()).collect(java.util.stream.Collectors.joining(File.pathSeparator));
        Process p=new ProcessBuilder(executavel,"-Dt4.raiz="+conexoes.config().raiz,"-cp",classpath,ProcessoInterrompido.class.getName(),""+conta,""+categoria,documento)
            .directory(conexoes.config().raiz.toFile()).start();
        // Drena as duas saidas enquanto a JVM trabalha, evitando bloquear o processo no pipe.
        var dados=CompletableFuture.supplyAsync(()->ler(p.getInputStream()));
        var erros=CompletableFuture.supplyAsync(()->ler(p.getErrorStream()));
        if(!p.waitFor(20,TimeUnit.SECONDS)){
            p.destroyForcibly();p.waitFor(5,TimeUnit.SECONDS);
            throw new IOException("Tempo limite excedido pelo processo auxiliar.");
        }
        byte[] bytes=dados.get(5,TimeUnit.SECONDS);
        String erro=new String(erros.get(5,TimeUnit.SECONDS),StandardCharsets.UTF_8);
        List<ResultadoSQL> passos=new ArrayList<>();
        if(bytes.length>0)try(ObjectInputStream entrada=new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object recebido=entrada.readObject();
            if(!(recebido instanceof List<?> lista))throw new IOException("Resposta invalida do processo auxiliar.");
            for(Object item:lista){
                if(!(item instanceof ResultadoSQL passo))throw new IOException("Resultado invalido do processo auxiliar.");
                passos.add(passo);registro.accept(passo);
            }
        }
        int codigo=p.exitValue();
        registro.accept(ResultadoSQL.comando("-- Processo auxiliar: exit "+codigo));
        if(codigo!=23||passos.stream().anyMatch(r->r.erroOriginal()!=null)) {
            if(passos.stream().noneMatch(r->r.erroOriginal()!=null)&&!erro.isBlank())
                registro.accept(ResultadoSQL.erro(null,List.of(),erro.strip(),null));
            return codigo;
        }
        if(passos.stream().noneMatch(r->LancamentoDAO.INSERT.equals(r.sql()))||passos.stream().noneMatch(r->ContaDAO.ATUALIZAR.equals(r.sql())))
            throw new IOException("Processo auxiliar nao devolveu os resultados do INSERT e do UPDATE.");
        return codigo;
    }
    private static byte[] ler(InputStream entrada){
        try(entrada){return entrada.readAllBytes();}catch(IOException e){throw new UncheckedIOException(e);}
    }
}
