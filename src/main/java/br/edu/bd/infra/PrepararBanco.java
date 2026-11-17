package br.edu.bd.infra;

import br.edu.bd.console.ConsoleEntrada;

import java.io.*;
import java.nio.file.*;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;

/** Prepara as tabelas do projeto e acrescenta os cadastros do MER na mesma transacao. */
public final class PrepararBanco {
    private static final String MARCA="Trabalho T4 individual - controle financeiro - versao 1";
    public static void main(String[] args) {
        ConsoleEntrada entrada = new ConsoleEntrada();
        try {
            console(new Configuracao(), entrada);
            entrada.linha("Banco preparado. Execute Aplicacao.java para abrir o menu no console.");
        } catch (EOFException | ConsoleEntrada.Cancelamento e) {
            entrada.linha("Preparacao cancelada.");
        } catch (Exception e) {
            entrada.linha("Falha na preparacao: " + ConsoleEntrada.erro(e));
            System.exit(1);
        }
    }
    public static void console(Configuracao c, ConsoleEntrada entrada) throws Exception {
        entrada.linha("PREPARAR POSTGRESQL | " + Configuracao.BANCO);
        entrada.linha("Enter aceita os valores sugeridos; :voltar cancela.");
        c.valores.setProperty("host", entrada.padrao("Servidor", c.host()));
        c.valores.setProperty("porta", "" + entrada.numero("Porta", 1, 65535, c.porta()));
        c.valores.setProperty("administrador", entrada.padrao("Usuario administrador", c.administrador()));
        String ambiente = System.getenv("T4_ADMIN_PASSWORD");
        char[] caracteres = ambiente == null ? entrada.senha() : ambiente.toCharArray();
        try { preparar(c,new String(caracteres)); } finally { Arrays.fill(caracteres,'\0'); }
    }
    public static void preparar(Configuracao cfg,String senha) throws Exception {
        Properties perfis=cfg.perfis();
        try(Connection admin=ConnectionFactory.conectar(cfg,"postgres",cfg.administrador(),senha)) {
            try(PreparedStatement s=admin.prepareStatement("SELECT oid,shobj_description(oid,'pg_database') FROM pg_database WHERE datname=?")) {
                s.setString(1,Configuracao.BANCO);
                try(ResultSet r=s.executeQuery()) {
                    if(r.next()) {
                        if(!MARCA.equals(r.getString(2))) throw new SQLException("Ja existe um banco com esse nome que nao foi criado por este projeto. Ele foi preservado.");
                    } else try(Statement st=admin.createStatement()) {
                        st.execute("CREATE DATABASE "+Configuracao.BANCO+" WITH ENCODING 'UTF8' TEMPLATE template0");
                        st.execute("COMMENT ON DATABASE "+Configuracao.BANCO+" IS '"+MARCA+"'");
                    }
                }
            }
            // Nao assumir o controle de logins preexistentes de outros projetos.
            for(String papel:List.of(Configuracao.ADMIN,Configuracao.LEITOR,Configuracao.OPERADOR)) {
                try(PreparedStatement s=admin.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname=?")) {
                    s.setString(1,papel);try(ResultSet r=s.executeQuery()) {
                        if(r.next() && !perfis.containsKey(papel)) throw new SQLException("Login "+papel+" ja existe sem credencial deste projeto. Nenhuma senha existente foi alterada.");
                    }
                }
                if(!perfis.containsKey(papel)) perfis.setProperty(papel,senhaAleatoria());
            }
        }
        // Salva antes da transacao para permitir repetir uma preparacao interrompida.
        cfg.salvarEndereco();
        try(Writer w=Files.newBufferedWriter(cfg.arquivoPerfis())) { perfis.store(w,"Senhas locais de demonstracao. NAO enviar ao GitHub."); }
        try(Connection c=ConnectionFactory.conectar(cfg,Configuracao.BANCO,cfg.administrador(),senha)) {
            c.setAutoCommit(false);
            try {
                executarArquivo(c,cfg.raiz.resolve("sql/01_estrutura.sql"));
                executarArquivo(c,cfg.raiz.resolve("sql/06_tabelas_mer.sql"));
                if(ConnectionFactory.numero(c,"SELECT count(*) FROM financeiro.empresa")==0) executarArquivo(c,cfg.raiz.resolve("sql/02_dados.sql"));
                executarArquivo(c,cfg.raiz.resolve("sql/03_seguranca.sql"));
                for(String perfil:List.of(Configuracao.ADMIN,Configuracao.LEITOR,Configuracao.OPERADOR)) {
                    // Nomes sao constantes internas; senha e gerada com caracteres alfanumericos.
                    try(Statement st=c.createStatement()) { st.execute("ALTER ROLE "+perfil+" PASSWORD '"+perfis.getProperty(perfil).replace("'","''")+"'"); }
                }
                c.commit();
            } catch(Exception e) { c.rollback();throw e; }
        }
        System.out.println("Preparacao concluida: "+cfg.host()+":"+cfg.porta()+" / "+Configuracao.BANCO);
    }
    static void executarArquivo(Connection c,Path arquivo) throws Exception {
        String texto=Files.readString(arquivo);
        for(String comando:texto.split("(?m)^-- @comando\\s*$"))
            if(!comando.isBlank()) try(Statement s=c.createStatement()) { s.execute(comando); }
    }
    private static String senhaAleatoria() {
        byte[] b=new byte[24];new SecureRandom().nextBytes(b);return HexFormat.of().formatHex(b);
    }
}
