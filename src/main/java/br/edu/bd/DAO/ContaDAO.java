package br.edu.bd.DAO;

import br.edu.bd.classes.casodeuso.Conta;
import br.edu.bd.classes.casodeuso.SaldoConta;
import br.edu.bd.classes.generico.Opcao;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public final class ContaDAO {
    public static final String SALDO="SELECT id,saldo_atual FROM financeiro.conta WHERE id=?";
    public static final String BLOQUEAR=SALDO+" FOR UPDATE";
    public static final String ATUALIZAR="UPDATE financeiro.conta SET saldo_atual=saldo_atual+? WHERE id=? RETURNING id,saldo_atual";
    public static final String CABECALHO="""
        SELECT c.id,c.empresa_id,e.razao_social,c.nome,c.saldo_inicial,c.saldo_atual
        FROM financeiro.conta c JOIN financeiro.empresa e ON e.id=c.empresa_id WHERE c.id=?
        """;
    public List<Opcao> listarOpcoes(Connection c,long empresa) throws SQLException {
        List<Opcao> itens=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT id,nome FROM financeiro.conta WHERE empresa_id=? ORDER BY id")){
            p.setLong(1,empresa);try(ResultSet r=p.executeQuery()){while(r.next())itens.add(new Opcao(r.getLong(1),r.getString(2)));}
        }return itens;
    }
    public Conta consultar(Connection c,long id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(CABECALHO)){
            p.setLong(1,id);try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Conta inexistente.");
                return new Conta(id,r.getLong(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6));
            }
        }
    }
    public SaldoConta consultarSaldo(Connection c,long conta) throws SQLException {return saldo(c,conta,SALDO);}
    public SaldoConta bloquear(Connection c,long conta) throws SQLException {return saldo(c,conta,BLOQUEAR);}
    private SaldoConta saldo(Connection c,long conta,String sql) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,conta);try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Conta inexistente.");
                return new SaldoConta(r.getLong(1),r.getBigDecimal(2));
            }
        }
    }
    public SaldoConta atualizarSaldo(Connection c,long conta,BigDecimal variacao) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(ATUALIZAR)){
            p.setBigDecimal(1,variacao);p.setLong(2,conta);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("Conta nao atualizada.");
                return new SaldoConta(r.getLong(1),r.getBigDecimal(2));
            }
        }
    }
}
