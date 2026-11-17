package br.edu.bd.classes.casodeuso;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados necessarios para registrar a receita ou despesa. */
public record Lancamento(long contaId, long categoriaId, LocalDate data, String documento,
                         String observacao, BigDecimal valor) { }
