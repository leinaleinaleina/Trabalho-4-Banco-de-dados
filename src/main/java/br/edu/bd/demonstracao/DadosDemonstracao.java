package br.edu.bd.demonstracao;

import br.edu.bd.classes.casodeuso.Empresa;
import br.edu.bd.classes.generico.Endereco;
import java.util.List;

/** Dados ficticios dos testes de permissoes; nao pertencem ao DAO. */
public final class DadosDemonstracao {
    private DadosDemonstracao(){ }
    public static Empresa empresa(String cnpj) {
        return new Empresa(cnpj,"Empresa de teste T4",new Endereco("Rua","Exemplo","10",null,"Centro","Curitiba","PR","80000000"),List.of(),List.of());
    }
}
