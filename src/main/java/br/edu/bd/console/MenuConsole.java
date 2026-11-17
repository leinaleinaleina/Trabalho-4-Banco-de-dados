package br.edu.bd.console;

import br.edu.bd.controller.*;
import br.edu.bd.classes.casodeuso.ResultadoTransacao.Modo;

/** Exibe e encaminha opcoes; nao acessa DAOs, servicos ou conexoes. */
public final class MenuConsole {
    private final ConsoleEntrada io;
    private final FinanceiroController financeiro;
    private final EmpresaController empresas;
    private final SegurancaController seguranca;
    private final String conexao;
    public MenuConsole(ConsoleEntrada io,FinanceiroController financeiro,EmpresaController empresas,SegurancaController seguranca,String conexao){this.io=io;this.financeiro=financeiro;this.empresas=empresas;this.seguranca=seguranca;this.conexao=conexao;}
    public void executar()throws Exception{
        io.titulo("CONTROLE DE DESPESAS E RECEITAS | T4A A T4D");
        io.linha(conexao);
        io.linha("Digite :voltar durante um cadastro para cancelar antes de gravar.");
        while(true){
            io.linha("\n1 - T4A | Consultar extrato de uma conta");
            io.linha("2 - T4A | Registrar receita ou despesa (INSERT + UPDATE)");
            io.linha("3 - T4B | Cadastrar empresa com endereco composto (INSERT)");
            io.linha("4 - T4B | Consultar empresas e enderecos (SELECT)");
            io.linha("5 - T4C | Login por perfil");
            io.linha("6 - T4D | COMMIT");
            io.linha("7 - T4D | Transacao incompleta - interrupcao abrupta");
            io.linha("8 - T4D | Transacao com ROLLBACK intencional");
            io.linha("9 - T4A | Consultar todos os extratos (apresentacao rapida)");
            io.linha("0 - Sair");
            try{
                switch(io.numero("Opcao",0,9,null)){
                    case 0->{io.linha("Programa encerrado.");return;}
                    case 1->financeiro.consultar();
                    case 2->financeiro.registrar();
                    case 3->empresas.cadastrar();
                    case 4->empresas.consultar();
                    case 5->seguranca.demonstrar();
                    case 6->financeiro.demonstrarTransacao(Modo.COMMIT);
                    case 7->financeiro.demonstrarTransacao(Modo.ABRUPTA);
                    case 8->financeiro.demonstrarTransacao(Modo.ROLLBACK);
                    case 9->financeiro.consultarTodos();
                    default->throw new IllegalStateException("Opcao inesperada.");
                }
            }catch(java.io.EOFException e){throw e;}
            catch(ConsoleEntrada.Cancelamento e){io.linha("Operacao cancelada. Retornando ao menu.");}
            catch(Exception e){io.linha("Operacao nao concluida: "+ConsoleEntrada.erro(e));}
        }
    }
}
