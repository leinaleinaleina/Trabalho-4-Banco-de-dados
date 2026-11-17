package br.edu.bd.classes.casodeuso;

import java.math.BigDecimal;
import java.util.List;
import br.edu.bd.classes.generico.ResultadoSQL;

public record ResultadoTransacao(Modo modo, long contaId, String documento, String natureza,
                                 BigDecimal antes, BigDecimal depois, long registros,
                                 List<ResultadoSQL> execucoes, Integer codigoProcesso, boolean concluida) {
    public enum Modo { COMMIT, ROLLBACK, ABRUPTA }
}
