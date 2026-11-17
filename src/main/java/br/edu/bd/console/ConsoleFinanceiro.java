package br.edu.bd.console;

import br.edu.bd.classes.casodeuso.*;
import br.edu.bd.classes.casodeuso.ResultadoTransacao.Modo;
import br.edu.bd.classes.generico.Opcao;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

/** Somente entrada e apresentacao; nao conhece Service, DAO ou Connection. */
public final class ConsoleFinanceiro {
    private final ConsoleEntrada io;
    private final NumberFormat moeda=NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    public ConsoleFinanceiro(ConsoleEntrada io){this.io=io;}

    public Opcao empresa(List<Opcao> empresas)throws IOException{return io.escolher("EMPRESA",empresas);}
    public Opcao conta(Opcao empresa,List<Opcao> contas)throws IOException{return io.escolher("CONTA DE "+empresa.nome(),contas);}
    public Categoria categoria(List<Categoria> categorias,String titulo)throws IOException{
        List<Opcao> opcoes=categorias.stream().map(c->new Opcao(c.id(),rotulo(c))).toList();
        long id=io.escolher(titulo,opcoes).id();
        return categorias.stream().filter(c->c.id()==id).findFirst().orElseThrow();
    }
    public void inicioRegistro(){io.titulo("T4A | REGISTRAR RECEITA OU DESPESA");}
    public void saldoAtual(Conta conta){io.linha("Saldo atual: "+moeda.format(conta.saldo()));}
    public Lancamento lerLancamento(long conta,List<Categoria> categorias)throws IOException{
        var data=io.data();
        var categoria=categoria(categorias,"CATEGORIA (R = receita; D = despesa)");
        String documento=io.texto("Numero do documento (unico nesta conta)",60);
        String obs=io.opcional("Observacoes",300);
        return new Lancamento(conta,categoria.id(),data,documento,obs,io.valor());
    }
    public boolean confirmarRegistro(Conta conta,Lancamento l,Categoria categoria)throws IOException{
        io.linha("\n"+conta.empresa()+" | "+conta.nome()+" | "+ConsoleEntrada.DATA.format(l.data()));
        io.linha("Categoria: "+rotulo(categoria)+" | Documento: "+l.documento()+" | Valor: "+moeda.format(l.valor()));
        return io.confirmar("Registrar e confirmar no banco?");
    }
    public void registroConcluido(Conta conta){
        io.linha("INSERT em lancamento e UPDATE em conta confirmados por COMMIT.");
        io.linha("Novo saldo: "+moeda.format(conta.saldo()));
    }
    public void extrato(Conta conta,List<Movimento> movimentos){
        io.titulo("T4A | EXTRATO");
        io.linha("Empresa: "+conta.empresa()+" | Conta: "+conta.nome()+" (ID "+conta.id()+")");
        io.linha("Saldo inicial: "+moeda.format(conta.inicial())+" | Saldo atual: "+moeda.format(conta.saldo()));
        io.linha(ConsoleEntrada.TRACO);
        io.linha(String.format("%-10s  %-24s  %-8s  %-18s  %13s  %13s","Data","Categoria","Natureza","Documento","Valor","Saldo"));
        for(var m:movimentos)io.linha(String.format("%-10s  %-24s  %-8s  %-18s  %13s  %13s",ConsoleEntrada.DATA.format(m.data()),curto(m.categoria(),24),m.natureza().equals("R")?"Receita":"Despesa",curto(m.documento(),18),moeda.format(m.valor()),moeda.format(m.saldo())));
        io.linha(ConsoleEntrada.TRACO);
        io.linha("Lancamentos: "+movimentos.size()+" | Saldo final: "+moeda.format(conta.saldo()));
        if(movimentos.isEmpty())io.linha("Nenhum lancamento cadastrado nesta conta.");
    }
    public void finalExtratos(int quantidade){io.linha("\nContas consultadas: "+quantidade+". Dados lidos do PostgreSQL.");}
    public void inicioTransacao(Modo modo){io.titulo("T4D | "+modo);}
    public boolean confirmarTransacao(Modo modo)throws IOException{
        if(modo!=Modo.COMMIT)return true;
        return io.confirmar("Confirmar lancamento de R$ 10,00 com COMMIT?");
    }
    public void resultadoTransacao(ResultadoTransacao resultado){io.linha(ConsoleDemonstracoes.formatar(resultado));}
    public void erroTransacao(Exception e){io.linha(e.toString());}
    private static String rotulo(Categoria c){return c.descricao()+" ("+c.natureza()+")";}
    private static String curto(String texto,int largura){return texto.length()<=largura?texto:texto.substring(0,largura-3)+"...";}
}
