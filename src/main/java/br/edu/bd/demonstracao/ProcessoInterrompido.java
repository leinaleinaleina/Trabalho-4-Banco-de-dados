package br.edu.bd.demonstracao;

import br.edu.bd.classes.casodeuso.Lancamento;
import br.edu.bd.classes.generico.ResultadoSQL;
import br.edu.bd.infra.*;
import br.edu.bd.service.FinanceiroService;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.io.ObjectOutputStream;
import java.util.*;

/** T4D(b): somente esta JVM auxiliar termina abruptamente. */
public final class ProcessoInterrompido {
    public static void main(String[] args)throws Exception {
        ConnectionFactory conexoes=new ConnectionFactory(new Configuracao());
        Lancamento lancamento=new Lancamento(Long.parseLong(args[0]),Long.parseLong(args[1]),LocalDate.now(),args[2],"Interrupcao abrupta T4D",new BigDecimal("10.00"));
        List<ResultadoSQL> passos=new ArrayList<>();
        try {
            // Intencionalmente sem close, commit, rollback ou finally no caminho de sucesso.
            Connection c=new FinanceiroService(conexoes).iniciarIncompleta(lancamento,passos::add);
            enviar(passos);
            java.lang.ref.Reference.reachabilityFence(c);
            Runtime.getRuntime().halt(23);
        }catch(Exception e){enviar(passos);throw e;}
    }
    private static void enviar(List<ResultadoSQL> passos)throws Exception {
        ObjectOutputStream saida=new ObjectOutputStream(System.out);
        saida.writeObject(List.copyOf(passos));saida.flush();
    }
}
