package br.edu.bd.classes.generico;

/** Representa o tipo composto financeiro.endereco_completo do PostgreSQL. */
public record Endereco(String tipo, String logradouro, String numero, String complemento,
                       String bairro, String cidade, String uf, String cep) { }
