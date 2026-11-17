package br.edu.bd.DAO;

import br.edu.bd.classes.casodeuso.Categoria;
import java.sql.*;
import java.util.*;

public final class CategoriaDAO {
    public static final String NATUREZA="SELECT natureza FROM financeiro.categoria WHERE id=?";
    public List<Categoria> listar(Connection c) throws SQLException {
        List<Categoria> categorias=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement("SELECT id,descricao,natureza FROM financeiro.categoria ORDER BY natureza DESC,descricao");ResultSet r=p.executeQuery()){
            while(r.next())categorias.add(new Categoria(r.getLong(1),r.getString(2),r.getString(3)));
        }return categorias;
    }
    public String natureza(Connection c,long id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(NATUREZA)){
            p.setLong(1,id);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Categoria inexistente.");return r.getString(1);}
        }
    }
}
