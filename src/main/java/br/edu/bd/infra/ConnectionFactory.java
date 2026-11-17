package br.edu.bd.infra;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/** Abre conexoes JDBC; os servicos controlam suas transacoes. */
public final class ConnectionFactory {
    private final Configuracao config;
    public ConnectionFactory(Configuracao config) { this.config=config; }
    public Configuracao config() { return config; }
    public Connection abrir() throws SQLException, IOException { return abrir(Configuracao.ADMIN); }
    public Connection abrir(String perfil) throws SQLException, IOException {
        String senha=config.perfis().getProperty(perfil);
        if (senha==null) throw new IOException("Perfil local ausente. Execute PrepararBanco.");
        return conectar(config,Configuracao.BANCO,perfil,senha);
    }
    public static Connection conectar(Configuracao c,String banco,String usuario,String senha) throws SQLException {
        Properties p=new Properties();
        p.setProperty("user",usuario); p.setProperty("password",senha);
        p.setProperty("connectTimeout","5"); p.setProperty("socketTimeout","30");
        p.setProperty("ApplicationName","Trabalho T4 - Controle financeiro");
        Connection cn=DriverManager.getConnection(c.url(banco),p);
        try (Statement s=cn.createStatement()) { s.execute("SET search_path TO financeiro,public"); s.execute("SET lock_timeout TO '5s'"); }
        return cn;
    }
    public static long numero(Connection c,String sql) throws SQLException {
        try (Statement s=c.createStatement(); ResultSet r=s.executeQuery(sql)) { r.next(); return r.getLong(1); }
    }
    public static String usuarioSemSuperpoderes(Connection c) throws SQLException {
        try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT current_user,rolsuper FROM pg_roles WHERE rolname=current_user")) {
            r.next();
            if(r.getBoolean(2)) throw new SQLException("O perfil da demonstracao nao pode ser superusuario.");
            return r.getString(1);
        }
    }
}
