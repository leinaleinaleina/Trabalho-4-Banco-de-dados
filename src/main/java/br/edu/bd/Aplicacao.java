package br.edu.bd;

import java.io.EOFException;
import br.edu.bd.console.*;
import br.edu.bd.controller.*;
import br.edu.bd.infra.*;
import br.edu.bd.service.*;

/**  todas as interacoes acontecem no console Run. */
public final class Aplicacao {
    public static void main(String[] args) {
        ConsoleEntrada console = new ConsoleEntrada();
        try {
            Configuracao config = new Configuracao();
            if (!config.preparada()) PrepararBanco.console(config, console);
            ConnectionFactory conexoes = new ConnectionFactory(config);
            EmpresaService empresas = new EmpresaService(conexoes);
            FinanceiroService financeiro = new FinanceiroService(conexoes);
            empresas.opcoes(); // Confere a conexao antes de exibir o menu.
            EmpresaController empresaController = new EmpresaController(empresas, new ConsoleEmpresa(console));
            FinanceiroController financeiroController = new FinanceiroController(financeiro, new TransacaoService(conexoes, financeiro), new ConsoleFinanceiro(console));
            SegurancaController segurancaController = new SegurancaController(new SegurancaService(conexoes), new ConsoleSeguranca(console));
            String endereco = "PostgreSQL " + config.host() + ":" + config.porta() + " | Banco: " + Configuracao.BANCO;
            new MenuConsole(console, financeiroController, empresaController, segurancaController, endereco).executar();
        } catch (EOFException e) {
            console.linha("\nEntrada encerrada. Nenhuma operacao pendente foi confirmada.");
        } catch (ConsoleEntrada.Cancelamento e) {
            console.linha("Preparacao cancelada.");
        } catch (Exception e) {
            console.linha("Nao foi possivel iniciar: " + ConsoleEntrada.erro(e));
            console.linha("Confira o PostgreSQL e execute PrepararBanco.java para revisar a conexao.");
            System.exit(1);
        }
    }
}
