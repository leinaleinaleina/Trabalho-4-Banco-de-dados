package br.edu.bd.classes.casodeuso;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado de uma linha do extrato; o saldo e calculado pelo SELECT. */
public record Movimento(LocalDate data, String categoria, String natureza,
                        String documento, BigDecimal valor, BigDecimal saldo) { }
