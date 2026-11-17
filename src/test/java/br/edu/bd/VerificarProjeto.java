package br.edu.bd;

import br.edu.bd.DAO.EmpresaDAO;
import br.edu.bd.classes.casodeuso.*;
import br.edu.bd.classes.casodeuso.ResultadoTransacao.Modo;
import br.edu.bd.classes.generico.Endereco;
import br.edu.bd.console.ConsoleDemonstracoes;
import br.edu.bd.demonstracao.DadosDemonstracao;
import br.edu.bd.infra.*;
import br.edu.bd.service.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Verificacao de integracao real. Execute em uma instalacao de demonstracao. */
public final class VerificarProjeto {
    public static void main(String[] args)throws Exception {
        Configuracao cfg=new Configuracao();
        if(Arrays.asList(args).contains("--preparar")) {
            String senha=System.getenv("T4_ADMIN_PASSWORD");
            if(senha==null)throw new IllegalArgumentException("Informe T4_ADMIN_PASSWORD apenas no ambiente local.");
            PrepararBanco.preparar(cfg,senha);
        }
        System.out.println(executar(new ConnectionFactory(cfg)));
    }
    public static String executar(ConnectionFactory banco)throws Exception {
        StringBuilder log=new StringBuilder("VERIFICACAO T4A A T4D\n");
        long conta,categoria;
        EmpresaDAO daoEmpresa=new EmpresaDAO();
        try(Connection c=banco.abrir()) {
            exigir(ConnectionFactory.numero(c,"SELECT count(*) FROM information_schema.tables WHERE table_schema='financeiro' AND table_type='BASE TABLE'")==14,"Quatorze tabelas criadas");
            exigir(ConnectionFactory.numero(c,"SELECT count(*) FROM financeiro.empresa")>=2,"Duas empresas");
            exigir(ConnectionFactory.numero(c,"SELECT count(*) FROM financeiro.conta")>=4,"Quatro contas");
            exigir(ConnectionFactory.numero(c,"SELECT count(*) FROM (SELECT c.id FROM financeiro.conta c JOIN financeiro.lancamento l ON l.conta_id=c.id GROUP BY c.id HAVING count(*)>=5) x")>=4,"Cinco lancamentos por conta");
            conta=ConnectionFactory.numero(c,"SELECT min(id) FROM financeiro.conta");categoria=ConnectionFactory.numero(c,"SELECT min(id) FROM financeiro.categoria WHERE natureza='R'");
            exigir(ConnectionFactory.numero(c,"SELECT count(*) FROM financeiro.conta c WHERE c.saldo_atual <> c.saldo_inicial + coalesce((SELECT sum(CASE k.natureza WHEN 'R' THEN l.valor ELSE -l.valor END) FROM financeiro.lancamento l JOIN financeiro.categoria k ON k.id=l.categoria_id WHERE l.conta_id=c.id),0)")==0,"Saldos consistentes");
            c.setAutoCommit(false);
            String cnpj="88"+String.format("%012d",System.currentTimeMillis()%1_000_000_000_000L);
            long id=daoEmpresa.inserir(c,DadosDemonstracao.empresa(cnpj));
            try(PreparedStatement p=c.prepareStatement("SELECT pg_typeof(endereco)::text,(endereco).cidade FROM financeiro.empresa WHERE id=?")) {
                p.setLong(1,id);try(ResultSet r=p.executeQuery()){r.next();exigir(r.getString(1).contains("endereco_completo")&&r.getString(2).equals("Curitiba"),"Composto nativo e SELECT dos campos");}
            }
            Savepoint ponto=c.setSavepoint();
            try {daoEmpresa.inserir(c,new Empresa("99999999999999","Invalida",new Endereco("Rua","X","1",null,"Centro","X",null,"00000000"),List.of(),List.of()));throw new IllegalStateException("Endereco invalido foi aceito");}
            catch(SQLException e){exigir("23514".equals(e.getSQLState()),"CHECK valida os campos internos");c.rollback(ponto);}
            c.rollback();
        }
        log.append("T4A: quatorze tabelas, dados minimos e saldos corretos.\nT4B: INSERT e SELECT do composto original de oito campos, com CHECK rejeitando endereco incompleto.\n");
        FinanceiroService servico=new FinanceiroService(banco);
        BigDecimal saldo=servico.conta(conta).saldo();
        String doc="VERIFICACAO-"+UUID.randomUUID();
        servico.registrar(new Lancamento(conta,categoria,LocalDate.now(),doc,"Verificacao automatizada",new BigDecimal("2.00")));
        try {servico.registrar(new Lancamento(conta,categoria,LocalDate.now(),doc,"Documento repetido",new BigDecimal("2.00")));throw new IllegalStateException("Documento duplicado aceito");}
        catch(SQLException e){exigir("23505".equals(e.getSQLState()),"Documento duplicado rejeitado");}
        exigir(servico.conta(conta).saldo().compareTo(saldo.add(new BigDecimal("2.00")))==0,"Falha no INSERT nao muda o saldo");
        // Remove apenas o lancamento temporario desta verificacao e compensa seu saldo atomicamente.
        try(Connection c=banco.abrir()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("DELETE FROM financeiro.lancamento WHERE conta_id=? AND documento=?")){p.setLong(1,conta);p.setString(2,doc);exigir(p.executeUpdate()==1,"Limpeza do teste");}
                try(PreparedStatement p=c.prepareStatement("UPDATE financeiro.conta SET saldo_atual=saldo_atual-2.00 WHERE id=?")){p.setLong(1,conta);p.executeUpdate();}c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
        for(Movimento m:servico.extrato(conta))exigir(m.saldo()!=null,"Saldo acumulado da consulta");
        var perfis=new SegurancaService(banco).executar();
        exigir(perfis.get(0).execucoes().stream().noneMatch(e->e.erroOriginal()!=null),"Administrador executa as modificacoes");
        exigir(perfis.get(1).execucoes().stream().anyMatch(e->"42501".equals(e.sqlState())),"PostgreSQL nega INSERT ao leitor");
        exigir(perfis.get(2).execucoes().stream().anyMatch(e->"42501".equals(e.sqlState())),"PostgreSQL nega DELETE ao operador");
        log.append(ConsoleDemonstracoes.formatar(perfis));
        TransacaoService d=new TransacaoService(banco,servico);
        for(Modo modo:Modo.values()) {
            var resultado=d.executar(modo,conta,categoria);
            exigir(resultado.concluida(),"Transacao "+modo+" sem erro SQL");
            var esperado=modo==Modo.COMMIT?resultado.antes().add(new BigDecimal("10.00")):resultado.antes();
            exigir(resultado.depois().compareTo(esperado)==0&&resultado.registros()==(modo==Modo.COMMIT?1:0),"Estado persistido de "+modo);
            if(modo==Modo.ABRUPTA)exigir(Integer.valueOf(23).equals(resultado.codigoProcesso()),"Interrupcao real da JVM auxiliar");
            log.append(ConsoleDemonstracoes.formatar(resultado)).append('\n');
        }
        log.append("TODAS AS VERIFICACOES PASSARAM.\nA demonstracao COMMIT acrescentou uma receita real de R$ 10,00 na conta de teste.\n");
        return log.toString();
    }
    private static void exigir(boolean condicao,String mensagem){if(!condicao)throw new IllegalStateException("Falha: "+mensagem);}
}
