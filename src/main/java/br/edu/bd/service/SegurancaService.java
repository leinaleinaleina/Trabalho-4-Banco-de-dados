package br.edu.bd.service;

import br.edu.bd.DAO.EmpresaDAO;
import br.edu.bd.classes.casodeuso.ResultadoPerfil;
import br.edu.bd.classes.casodeuso.ResultadoPerfil.ExecucaoSQL;
import br.edu.bd.classes.casodeuso.RegistroEmpresa;
import br.edu.bd.demonstracao.DadosDemonstracao;
import br.edu.bd.infra.*;
import java.sql.*;
import java.util.*;

/** T4C: autentica o login escolhido e deixa o PostgreSQL permitir ou negar os SQLs. */
public final class SegurancaService {
    private final ConnectionFactory conexoes;
    private final EmpresaDAO empresas=new EmpresaDAO();
    public SegurancaService(ConnectionFactory conexoes){this.conexoes=conexoes;}

    public List<ResultadoPerfil> executar() throws Exception {
        List<ResultadoPerfil> resultados=new ArrayList<>();
        for(int id=1;id<=3;id++)resultados.add(executar(id));
        return resultados;
    }

    public ResultadoPerfil executar(int id) throws Exception {
        String perfil=switch(id){
            case 1->Configuracao.ADMIN;
            case 2->Configuracao.LEITOR;
            case 3->Configuracao.OPERADOR;
            default->throw new IllegalArgumentException("ID de login invalido. Escolha 1, 2 ou 3.");
        };
        String cnpj="77"+String.format("%012d",System.currentTimeMillis()%1_000_000_000_000L);
        var dadosTeste=DadosDemonstracao.empresa(cnpj);
        List<ExecucaoSQL> execucoes=new ArrayList<>();
        try(Connection c=conexoes.abrir(perfil)) {
            c.setAutoCommit(false);
            try {
                String login=ConnectionFactory.usuarioSemSuperpoderes(c);
                execucoes.add(comando("BEGIN"));
                execucoes.add(resultado(EmpresaDAO.SELECT_PERFIL,List.of(),empresas.consultarPerfil(c,null)));
                var existentes=empresas.consultar(c);
                if(existentes.isEmpty())throw new SQLException("Cadastre uma empresa antes de demonstrar os perfis.");
                var empresaTeste=new br.edu.bd.classes.casodeuso.Empresa(cnpj,dadosTeste.nome(),existentes.get(0).endereco(),List.of(),List.of());
                var parametros=EmpresaDAO.parametrosInsercao(empresaTeste);
                if(perfil.equals(Configuracao.LEITOR)) {
                    tentar(c,execucoes,EmpresaDAO.INSERT_PERFIL,parametros,()->empresas.inserirPerfil(c,empresaTeste));
                } else {
                    RegistroEmpresa inserida=empresas.inserirPerfil(c,empresaTeste);
                    execucoes.add(resultado(EmpresaDAO.INSERT_PERFIL,parametros,List.of(inserida)));
                    if(perfil.equals(Configuracao.ADMIN)) {
                        RegistroEmpresa atualizada=empresas.atualizarNome(c,inserida.id(),"Teste de permissao");
                        execucoes.add(resultado(EmpresaDAO.UPDATE_PERFIL,List.of("Teste de permissao",inserida.id()),List.of(atualizada)));
                        RegistroEmpresa excluida=empresas.excluir(c,inserida.id());
                        execucoes.add(resultado(EmpresaDAO.DELETE_PERFIL,List.of(inserida.id()),List.of(excluida)));
                    } else {
                        tentar(c,execucoes,EmpresaDAO.DELETE_PERFIL,List.of(inserida.id()),()->empresas.excluir(c,inserida.id()));
                    }
                    execucoes.add(resultado(EmpresaDAO.SELECT_PERFIL_ID,List.of(inserida.id()),empresas.consultarPerfil(c,inserida.id())));
                }
                c.rollback();
                execucoes.add(comando("ROLLBACK"));
                return new ResultadoPerfil(id,login,List.copyOf(execucoes));
            } catch(Exception e) {
                try{c.rollback();}catch(SQLException falha){e.addSuppressed(falha);}
                throw e;
            }
        }
    }
    private static ExecucaoSQL comando(String sql){return new ExecucaoSQL(sql,List.of(),null,null,null);}
    private static ExecucaoSQL resultado(String sql,List<Object> parametros,List<RegistroEmpresa> registros){
        return new ExecucaoSQL(sql,parametros,List.copyOf(registros),null,null);
    }
    @FunctionalInterface private interface Acao{RegistroEmpresa executar()throws SQLException;}
    private static void tentar(Connection c,List<ExecucaoSQL> execucoes,String sql,List<Object> parametros,Acao acao)throws SQLException {
        Savepoint ponto=c.setSavepoint("t4c_permissao");
        execucoes.add(comando("SAVEPOINT t4c_permissao"));
        try {
            execucoes.add(resultado(sql,parametros,List.of(acao.executar())));
        } catch(SQLException e) {
            execucoes.add(new ExecucaoSQL(sql,parametros,null,e.toString(),e.getSQLState()));
            c.rollback(ponto);
            execucoes.add(comando("ROLLBACK TO SAVEPOINT t4c_permissao"));
        } finally {
            c.releaseSavepoint(ponto);
            execucoes.add(comando("RELEASE SAVEPOINT t4c_permissao"));
        }
    }
}
