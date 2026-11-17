package br.edu.bd.console;

import br.edu.bd.classes.casodeuso.ResultadoPerfil;
import java.io.IOException;

/** Menu de selecao dos logins e apresentacao dos resultados do PostgreSQL. */
public final class ConsoleSeguranca {
    private final ConsoleEntrada io;
    public ConsoleSeguranca(ConsoleEntrada io){this.io=io;}

    public int escolherLogin() throws IOException {
        io.titulo("T4C | LOGIN POR PERFIL");
        io.linha("1 - Administrador");
        io.linha("2 - Leitor");
        io.linha("3 - Operador");
        io.linha("0 - Voltar ao menu principal");
        return io.numero("ID do login",0,3,null);
    }

    public void resultado(ResultadoPerfil perfil){io.linha(ConsoleDemonstracoes.formatar(perfil));}
    public void erro(Exception e){io.linha(e.toString());}
}
