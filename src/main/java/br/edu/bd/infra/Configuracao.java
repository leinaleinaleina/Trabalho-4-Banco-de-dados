package br.edu.bd.infra;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Endereco do servidor e credenciais locais. Nenhuma senha e incluida no Git. */
public final class Configuracao {
    public static final String BANCO = "bd_t4_individual";
    public static final String ADMIN = "bdt4_ind_admin", LEITOR = "bdt4_ind_leitor", OPERADOR = "bdt4_ind_operador";
    public final Path raiz;
    public final Properties valores = new Properties();
    public Configuracao() throws IOException {
        raiz = localizar();
        ler(raiz.resolve("conexao.properties"), valores);
        ler(raiz.resolve(".local/conexao.properties"), valores);
        for (String k : List.of("host", "porta", "administrador")) {
            String v = System.getProperty("t4." + k, System.getenv("T4_" + k.toUpperCase(Locale.ROOT)));
            if (v != null) valores.setProperty(k, v);
        }
    }
    private static Path localizar() throws IOException {
        Path inicio = Path.of(System.getProperty("t4.raiz", ".")).toAbsolutePath().normalize();
        for (Path p = inicio; p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("sql/01_estrutura.sql")) && Files.isRegularFile(p.resolve("pom.xml"))) return p;
            Path filho = p.resolve("T4-Individual");
            if (Files.isRegularFile(filho.resolve("sql/01_estrutura.sql"))) return filho;
        }
        throw new IOException("Abra T4-Individual/pom.xml no IntelliJ e execute a partir desse projeto.");
    }
    public static void ler(Path arquivo, Properties p) throws IOException {
        if (Files.isRegularFile(arquivo)) try (Reader r=Files.newBufferedReader(arquivo)) { p.load(r); }
    }
    public String host() { return valores.getProperty("host", "127.0.0.1"); }
    public int porta() { return Integer.parseInt(valores.getProperty("porta", "5432")); }
    public String administrador() { return valores.getProperty("administrador", "postgres"); }
    public String url(String banco) { return "jdbc:postgresql://"+host()+":"+porta()+"/"+banco; }
    public Path arquivoPerfis() { return raiz.resolve(".local/perfis.properties"); }
    public Properties perfis() throws IOException { Properties p=new Properties(); ler(arquivoPerfis(),p); return p; }
    public boolean preparada() { return Files.isRegularFile(arquivoPerfis()); }
    public void salvarEndereco() throws IOException {
        Files.createDirectories(raiz.resolve(".local"));
        try (Writer w=Files.newBufferedWriter(raiz.resolve(".local/conexao.properties"))) {
            valores.store(w,"Configuracao exclusiva deste computador; sem senha administrativa.");
        }
    }
}
