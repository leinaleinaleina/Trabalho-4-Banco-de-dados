package br.edu.bd.DAO;

import br.edu.bd.classes.casodeuso.Empresa;
import br.edu.bd.classes.casodeuso.RegistroEmpresa;
import br.edu.bd.classes.generico.*;
import java.sql.*;
import java.util.*;

/** SQL de empresa e seus contatos; nao abre/fecha conexoes nem controla transacoes. */
public final class EmpresaDAO {
    public static final String INSERT = """
        INSERT INTO financeiro.empresa(cnpj,razao_social,endereco)
        VALUES (?, ?, ROW(?,?,?,?,?,?,?,?)::financeiro.endereco_completo) RETURNING id
        """;
    public static final String INSERT_PERFIL = INSERT.replace("RETURNING id", "RETURNING id,cnpj,razao_social");
    public static final String SELECT_PERFIL = "SELECT id,cnpj,razao_social FROM financeiro.empresa ORDER BY id LIMIT 10";
    public static final String SELECT_PERFIL_ID = "SELECT id,cnpj,razao_social FROM financeiro.empresa WHERE id=?";
    public static final String UPDATE_PERFIL = "UPDATE financeiro.empresa SET razao_social=? WHERE id=? RETURNING id,cnpj,razao_social";
    public static final String DELETE_PERFIL = "DELETE FROM financeiro.empresa WHERE id=? RETURNING id,cnpj,razao_social";
    public static final String SELECT = """
        SELECT e.id,e.cnpj,e.razao_social,(e.endereco).tipo_logradouro,(e.endereco).logradouro,
          (e.endereco).numero,(e.endereco).complemento,(e.endereco).bairro,(e.endereco).cidade,
          (e.endereco).uf,(e.endereco).cep,
          ARRAY(SELECT m.email FROM financeiro.email_empresa m WHERE m.empresa_id=e.id ORDER BY m.id) AS emails,
          ARRAY(SELECT t.ddi||' '||t.ddd||' '||t.numero FROM financeiro.telefone_empresa t
                WHERE t.empresa_id=e.id ORDER BY t.id) AS telefones
        FROM financeiro.empresa e ORDER BY e.id
        """;

    public List<Opcao> listarOpcoes(Connection c) throws SQLException {
        List<Opcao> itens = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement("SELECT id,razao_social FROM financeiro.empresa ORDER BY razao_social"); ResultSet r=p.executeQuery()) {
            while(r.next()) itens.add(new Opcao(r.getLong(1),r.getString(2)));
        }
        return itens;
    }
    public long inserir(Connection c, Empresa empresa) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(INSERT)) {
            preencherInsercao(p,empresa);
            try(ResultSet r=p.executeQuery()) {r.next();return r.getLong(1);}
        }
    }
    public static List<Object> parametrosInsercao(Empresa empresa) {
        Endereco a=empresa.endereco();
        return Arrays.asList(empresa.cnpj(),empresa.nome(),a.tipo(),a.logradouro(),a.numero(),a.complemento(),a.bairro(),a.cidade(),a.uf(),a.cep());
    }
    private void preencherInsercao(PreparedStatement p,Empresa empresa) throws SQLException {
        var parametros=parametrosInsercao(empresa);
        for(int i=0;i<parametros.size();i++)p.setString(i+1,(String)parametros.get(i));
    }
    public void inserirEmail(Connection c,long empresa,Email email) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO financeiro.email_empresa(empresa_id,email) VALUES (?,?)")) {
            p.setLong(1,empresa);p.setString(2,email.valor().trim());p.executeUpdate();
        }
    }
    public void inserirTelefone(Connection c,long empresa,Telefone telefone) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO financeiro.telefone_empresa(empresa_id,ddi,ddd,numero) VALUES (?,?,?,?)")) {
            p.setLong(1,empresa);p.setString(2,telefone.ddi());p.setString(3,telefone.ddd());p.setString(4,telefone.numero());p.executeUpdate();
        }
    }
    public List<Empresa> consultar(Connection c) throws SQLException {
        List<Empresa> empresas=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(SELECT);ResultSet r=p.executeQuery()) {
            while(r.next()) {
                Endereco endereco=new Endereco(r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8),r.getString(9),r.getString(10),r.getString(11));
                List<Email> emails=textos(r,"emails").stream().map(Email::new).toList();
                List<Telefone> telefones=textos(r,"telefones").stream().map(t->{String[] partes=t.split(" ",3);return new Telefone(partes[0],partes[1],partes[2]);}).toList();
                empresas.add(new Empresa(r.getLong(1),r.getString(2),r.getString(3),endereco,emails,telefones));
            }
        }
        return empresas;
    }
    private List<String> textos(ResultSet r,String coluna) throws SQLException {
        java.sql.Array valores=r.getArray(coluna);
        try { return Arrays.asList((String[])valores.getArray()); } finally { valores.free(); }
    }
    // SELECT e RETURNING fornecem ao T4C os dados reais antes/depois das modificacoes.
    public List<RegistroEmpresa> consultarPerfil(Connection c,Long id) throws SQLException {
        List<RegistroEmpresa> registros=new ArrayList<>();
        try(PreparedStatement p=c.prepareStatement(id==null?SELECT_PERFIL:SELECT_PERFIL_ID)) {
            if(id!=null)p.setLong(1,id);
            try(ResultSet r=p.executeQuery()){while(r.next())registros.add(registro(r));}
        }
        return registros;
    }
    public RegistroEmpresa inserirPerfil(Connection c,Empresa empresa) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(INSERT_PERFIL)) {
            preencherInsercao(p,empresa);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("INSERT nao retornou o registro da empresa.");
                return registro(r);
            }
        }
    }
    public RegistroEmpresa atualizarNome(Connection c,long id,String nome) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(UPDATE_PERFIL)) {
            p.setString(1,nome);p.setLong(2,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("UPDATE nao encontrou a empresa.");
                return registro(r);
            }
        }
    }
    public RegistroEmpresa excluir(Connection c,long id) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(DELETE_PERFIL)) {
            p.setLong(1,id);
            try(ResultSet r=p.executeQuery()) {
                if(!r.next())throw new SQLException("DELETE nao encontrou a empresa.");
                return registro(r);
            }
        }
    }
    private RegistroEmpresa registro(ResultSet r) throws SQLException {
        return new RegistroEmpresa(r.getLong(1),r.getString(2),r.getString(3));
    }
}
