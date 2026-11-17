package br.edu.bd.service;

import br.edu.bd.DAO.*;
import br.edu.bd.classes.casodeuso.*;
import br.edu.bd.classes.generico.Opcao;
import br.edu.bd.classes.generico.ResultadoSQL;
import br.edu.bd.infra.ConnectionFactory;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/** Regra financeira e unidade da transacao; todos os DAOs recebem a mesma Connection. */
public final class FinanceiroService {
    private final ConnectionFactory conexoes;
    private final EmpresaDAO empresas=new EmpresaDAO();
    private final ContaDAO contas=new ContaDAO();
    private final CategoriaDAO categorias=new CategoriaDAO();
    private final LancamentoDAO lancamentos=new LancamentoDAO();
    public FinanceiroService(ConnectionFactory conexoes){this.conexoes=conexoes;}

    public List<Opcao> empresas() throws Exception {
        try(Connection c=conexoes.abrir()){return empresas.listarOpcoes(c);}
    }
    public List<Opcao> contas(long empresa) throws Exception {
        try(Connection c=conexoes.abrir()){return contas.listarOpcoes(c,empresa);}
    }
    public List<Categoria> categorias() throws Exception {
        try(Connection c=conexoes.abrir()){return categorias.listar(c);}
    }
    public Conta conta(long id) throws Exception {
        try(Connection c=conexoes.abrir()){return contas.consultar(c,id);}
    }
    public List<Movimento> extrato(long id) throws Exception {
        try(Connection c=conexoes.abrir()){return lancamentos.extrato(c,id);}
    }
    public void registrar(Lancamento lancamento) throws Exception { executarTransacao(lancamento,true,r->{}); }
    public void registrarComRollback(Lancamento lancamento) throws Exception { executarTransacao(lancamento,false,r->{}); }

    public void executarTransacao(Lancamento lancamento,boolean confirmar,Consumer<ResultadoSQL> registro) throws Exception {
        try(Connection c=conexoes.abrir()) {
            c.setAutoCommit(false);
            registro.accept(ResultadoSQL.comando("BEGIN"));
            try {
                modificar(c,lancamento,registro);
                controlar(c,confirmar,registro);
            } catch(Exception e) {
                try{controlar(c,false,registro);}catch(SQLException falha){e.addSuppressed(falha);}
                throw e;
            }
        }
    }
    private void modificar(Connection c,Lancamento l,Consumer<ResultadoSQL> registro) throws SQLException {
        if(c.getAutoCommit())throw new IllegalStateException("A operacao exige uma transacao aberta pelo servico.");
        if(l.valor()==null||l.valor().signum()<=0||l.valor().scale()>2)
            throw new IllegalArgumentException("Informe um valor positivo com ate duas casas decimais.");
        executarSQL(registro,ContaDAO.BLOQUEAR,List.of(l.contaId()),List.of("id","saldo_atual"),
            ()->contas.bloquear(c,l.contaId()),FinanceiroService::linhaSaldo);
        String natureza=executarSQL(registro,CategoriaDAO.NATUREZA,List.of(l.categoriaId()),List.of("natureza"),
            ()->categorias.natureza(c,l.categoriaId()),n->List.of(List.of(n)));
        BigDecimal variacao=natureza.equals("R")?l.valor():l.valor().negate();
        executarSQL(registro,LancamentoDAO.INSERT,parametros(l),COLUNAS_LANCAMENTO,
            ()->lancamentos.inserir(c,l),r->List.of(linhaLancamento(r)));
        executarSQL(registro,ContaDAO.ATUALIZAR,List.of(variacao,l.contaId()),List.of("id","saldo_atual"),
            ()->contas.atualizarSaldo(c,l.contaId(),variacao),FinanceiroService::linhaSaldo);
    }
    static final List<String> COLUNAS_LANCAMENTO=List.of("id","conta_id","categoria_id","data_lancamento","documento","observacao","valor");
    static List<Object> parametros(Lancamento l){return Arrays.asList(l.contaId(),l.categoriaId(),l.data(),l.documento(),l.observacao(),l.valor());}
    static List<Object> linhaLancamento(RegistroLancamento registro){
        var l=registro.lancamento();
        return Arrays.asList(registro.id(),l.contaId(),l.categoriaId(),l.data(),l.documento(),l.observacao(),l.valor());
    }
    static List<List<Object>> linhaSaldo(SaldoConta saldo){return List.of(List.of(saldo.id(),saldo.saldo()));}
    @FunctionalInterface interface OperacaoSQL<T>{T executar()throws SQLException;}
    static <T> T executarSQL(Consumer<ResultadoSQL> registro,String sql,List<Object> parametros,List<String> colunas,
                            OperacaoSQL<T> operacao,Function<T,List<List<Object>>> linhas)throws SQLException {
        try {
            T resultado=operacao.executar();
            registro.accept(new ResultadoSQL(sql,parametros,colunas,linhas.apply(resultado),null,null));
            return resultado;
        }catch(SQLException e){
            registro.accept(ResultadoSQL.erro(sql,parametros,e.toString(),e.getSQLState()));
            throw e;
        }
    }
    private static void controlar(Connection c,boolean confirmar,Consumer<ResultadoSQL> registro)throws SQLException {
        String sql=confirmar?"COMMIT":"ROLLBACK";
        try {
            if(confirmar)c.commit();else c.rollback();
            registro.accept(ResultadoSQL.comando(sql));
        }catch(SQLException e){registro.accept(ResultadoSQL.erro(sql,List.of(),e.toString(),e.getSQLState()));throw e;}
    }
    /** Exclusivo T4D(b): a JVM auxiliar deve terminar com esta conexao ainda aberta. */
    public Connection iniciarIncompleta(Lancamento lancamento) throws Exception {
        return iniciarIncompleta(lancamento,r->{});
    }
    public Connection iniciarIncompleta(Lancamento lancamento,Consumer<ResultadoSQL> registro) throws Exception {
        Connection c=conexoes.abrir();
        try {
            c.setAutoCommit(false);registro.accept(ResultadoSQL.comando("BEGIN"));
            modificar(c,lancamento,registro);return c;
        } catch(Exception e) {
            try{controlar(c,false,registro);}catch(SQLException falha){e.addSuppressed(falha);}
            try{c.close();}catch(SQLException falha){e.addSuppressed(falha);}
            throw e;
        }
    }
}
