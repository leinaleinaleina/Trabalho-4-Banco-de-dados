package br.edu.bd.classes.casodeuso;

/** Identificador e campos efetivamente devolvidos pelo PostgreSQL. */
public record RegistroLancamento(long id, Lancamento lancamento) { }
