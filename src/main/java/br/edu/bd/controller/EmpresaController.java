package br.edu.bd.controller;

import br.edu.bd.console.*;
import br.edu.bd.service.EmpresaService;

/** Conduz o cadastro/consulta; nao decide COMMIT e nao executa SQL. */
public final class EmpresaController {
    private final EmpresaService servico;
    private final ConsoleEmpresa console;
    public EmpresaController(EmpresaService servico,ConsoleEmpresa console){this.servico=servico;this.console=console;}
    public void cadastrar()throws Exception{
        console.inicioCadastro();
        var empresa=console.lerCadastro();
        if(!console.confirmarCadastro(empresa))throw new ConsoleEntrada.Cancelamento();
        long id=servico.cadastrar(empresa);
        console.cadastroConcluido(id);
        for(var cadastrada:servico.consultar())if(cadastrada.id()==id)console.exibir(cadastrada);
    }
    public void consultar()throws Exception{console.listar(servico.consultar());}
}
