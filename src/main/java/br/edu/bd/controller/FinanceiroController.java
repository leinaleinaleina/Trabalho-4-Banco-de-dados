package br.edu.bd.controller;

import br.edu.bd.classes.casodeuso.ResultadoTransacao.Modo;
import br.edu.bd.console.*;
import br.edu.bd.service.*;

/** Escolhas e sequencia das interacoes; regras e transacoes ficam nos servicos. */
public final class FinanceiroController {
    private final FinanceiroService financeiro;
    private final TransacaoService transacoes;
    private final ConsoleFinanceiro console;
    public FinanceiroController(FinanceiroService financeiro,TransacaoService transacoes,ConsoleFinanceiro console){this.financeiro=financeiro;this.transacoes=transacoes;this.console=console;}
    private long escolherConta()throws Exception{
        var empresa=console.empresa(financeiro.empresas());
        return console.conta(empresa,financeiro.contas(empresa.id())).id();
    }
    public void consultar()throws Exception{exibirExtrato(escolherConta());}
    private void exibirExtrato(long id)throws Exception{console.extrato(financeiro.conta(id),financeiro.extrato(id));}
    public void consultarTodos()throws Exception{
        int quantidade=0;
        for(var empresa:financeiro.empresas())for(var conta:financeiro.contas(empresa.id())){exibirExtrato(conta.id());quantidade++;}
        console.finalExtratos(quantidade);
    }
    public void registrar()throws Exception{
        console.inicioRegistro();
        long id=escolherConta();
        var conta=financeiro.conta(id);console.saldoAtual(conta);
        var categorias=financeiro.categorias();
        var lancamento=console.lerLancamento(id,categorias);
        var categoria=categorias.stream().filter(c->c.id()==lancamento.categoriaId()).findFirst().orElseThrow();
        if(!console.confirmarRegistro(conta,lancamento,categoria))throw new ConsoleEntrada.Cancelamento();
        financeiro.registrar(lancamento);
        console.registroConcluido(financeiro.conta(id));
    }
    public void demonstrarTransacao(Modo modo)throws Exception{
        try {
            console.inicioTransacao(modo);
            long conta=escolherConta();
            var categoria=console.categoria(financeiro.categorias(),"CATEGORIA");
            if(!console.confirmarTransacao(modo))throw new ConsoleEntrada.Cancelamento();
            console.resultadoTransacao(transacoes.executar(modo,conta,categoria.id()));
        }
        catch(java.sql.SQLException e){console.erroTransacao(e);}
    }
}
