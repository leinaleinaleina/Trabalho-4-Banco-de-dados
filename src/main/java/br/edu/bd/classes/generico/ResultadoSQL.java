package br.edu.bd.classes.generico;

import java.io.Serializable;
import java.util.List;

/** Comando e resultado observados; nao executa SQL nem formata o console. */
public record ResultadoSQL(String sql, List<Object> parametros, List<String> colunas,
                           List<List<Object>> linhas, String erroOriginal, String sqlState) implements Serializable {
    public static ResultadoSQL comando(String sql) {
        return new ResultadoSQL(sql,List.of(),List.of(),null,null,null);
    }
    public static ResultadoSQL erro(String sql,List<Object> parametros,String erro,String estado) {
        return new ResultadoSQL(sql,parametros,List.of(),null,erro,estado);
    }
}
