package br.edu.bd.DAO;

import br.edu.bd.classes.casodeuso.*;
import java.sql.*;
import java.util.*;

public final class LancamentoDAO {
    public static final String CAMPOS="id,conta_id,categoria_id,data_lancamento,documento,observacao,valor";
    public static final String INSERT="""
        INSERT INTO financeiro.lancamento(conta_id,categoria_id,data_lancamento,documento,observacao,valor)
        VALUES (?,?,?,?,?,?)
        """+"RETURNING "+CAMPOS;
    public static final String POR_DOCUMENTO="SELECT "+CAMPOS+"\nFROM financeiro.lancamento\nWHERE conta_id=? AND documento=?";
    public static final String EXTRATO="""
        SELECT l.data_lancamento,k.descricao,k.natureza,l.documento,l.valor,
          c.saldo_inicial + sum(CASE k.natureza WHEN 'R' THEN l.valor ELSE -l.valor END)
          OVER (ORDER BY l.data_lancamento,l.id ROWS UNBOUNDED PRECEDING) AS saldo
        FROM financeiro.lancamento l JOIN financeiro.categoria k ON k.id=l.categoria_id
        JOIN financeiro.conta c ON c.id=l.conta_id WHERE c.id=? ORDER BY l.data_lancamento,l.id
        """;
    public RegistroLancamento inserir(Connection c,Lancamento l) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(INSERT)){
            p.setLong(1,l.contaId());p.setLong(2,l.categoriaId());p.setDate(3,java.sql.Date.valueOf(l.data()));p.setString(4,l.documento());p.setString(5,l.observacao());p.setBigDecimal(6,l.valor());
            try(ResultSet r=p.executeQuery()){
                if(!r.next())throw new SQLException("INSERT nao retornou o lancamento.");
                return registro(r);
            }
        }
    }
    public List<RegistroLancamento> consultarDocumento(Connection c,long conta,String documento) throws SQLException {
        List<RegistroLancamento> registros=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(POR_DOCUMENTO)){
            p.setLong(1,conta);p.setString(2,documento);
            try(ResultSet r=p.executeQuery()){while(r.next())registros.add(registro(r));}
        }
        return registros;
    }
    private RegistroLancamento registro(ResultSet r)throws SQLException {
        return new RegistroLancamento(r.getLong("id"),new Lancamento(r.getLong("conta_id"),r.getLong("categoria_id"),
            r.getDate("data_lancamento").toLocalDate(),r.getString("documento"),r.getString("observacao"),r.getBigDecimal("valor")));
    }
    public List<Movimento> extrato(Connection c,long conta) throws SQLException {
        List<Movimento> linhas=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(EXTRATO)){
            p.setLong(1,conta);try(ResultSet r=p.executeQuery()){
                while(r.next())linhas.add(new Movimento(r.getDate(1).toLocalDate(),r.getString(2),r.getString(3),r.getString(4),r.getBigDecimal(5),r.getBigDecimal(6)));
            }
        }return linhas;
    }
    public long contarDocumento(Connection c,long conta,String documento) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT count(*) FROM financeiro.lancamento WHERE conta_id=? AND documento=?")){
            p.setLong(1,conta);p.setString(2,documento);try(ResultSet r=p.executeQuery()){r.next();return r.getLong(1);}
        }
    }
}
