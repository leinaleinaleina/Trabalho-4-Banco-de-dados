package br.edu.bd.classes.casodeuso;

import br.edu.bd.classes.generico.*;
import java.util.List;

public record Empresa(long id, String cnpj, String nome, Endereco endereco,
                      List<Email> emails, List<Telefone> telefones) {
    public Empresa(String cnpj, String nome, Endereco endereco, List<Email> emails, List<Telefone> telefones) {
        this(0, cnpj, nome, endereco, emails, telefones);
    }
}
