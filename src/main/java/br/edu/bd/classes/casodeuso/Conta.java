package br.edu.bd.classes.casodeuso;

import java.math.BigDecimal;

/** Dados da conta e nome da empresa obtidos pela consulta de cabecalho. */
public record Conta(long id, long empresaId, String empresa, String nome,
                    BigDecimal inicial, BigDecimal saldo) { }
