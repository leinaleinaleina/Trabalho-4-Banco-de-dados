package br.edu.bd.classes.casodeuso;

/** Linha realmente devolvida pelo SELECT ou RETURNING no T4C. */
public record RegistroEmpresa(long id, String cnpj, String razaoSocial) { }
