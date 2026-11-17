package br.edu.bd.service;

import br.edu.bd.DAO.EmpresaDAO;
import br.edu.bd.classes.casodeuso.Empresa;
import br.edu.bd.classes.generico.Opcao;
import br.edu.bd.infra.ConnectionFactory;
import java.sql.*;
import java.util.List;

/** Confirma ou desfaz o cadastro inteiro, sem depender do console. */
public final class EmpresaService {
    private final ConnectionFactory conexoes;
    private final EmpresaDAO dao=new EmpresaDAO();
    public EmpresaService(ConnectionFactory conexoes){this.conexoes=conexoes;}

    public long cadastrar(Empresa empresa) throws Exception {
        try(Connection c=conexoes.abrir()) {
            c.setAutoCommit(false);
            try {
                long id=dao.inserir(c,empresa);
                for(var email:empresa.emails())dao.inserirEmail(c,id,email);
                for(var telefone:empresa.telefones())dao.inserirTelefone(c,id,telefone);
                c.commit();return id;
            } catch(Exception e) {
                try{c.rollback();}catch(SQLException falha){e.addSuppressed(falha);}
                throw e;
            }
        }
    }
    public List<Empresa> consultar() throws Exception {
        try(Connection c=conexoes.abrir()){return dao.consultar(c);}
    }
    public List<Opcao> opcoes() throws Exception {
        try(Connection c=conexoes.abrir()){return dao.listarOpcoes(c);}
    }
}
