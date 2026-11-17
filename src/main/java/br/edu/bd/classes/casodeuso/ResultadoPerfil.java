package br.edu.bd.classes.casodeuso;

import java.util.List;

public record ResultadoPerfil(int id, String login, List<ExecucaoSQL> execucoes) {
    /** Registros nulos significam comando sem ResultSet, como SAVEPOINT ou ROLLBACK. */
    public record ExecucaoSQL(String sql, List<Object> parametros, List<RegistroEmpresa> registros,
                             String erroOriginal, String sqlState) { }
}
