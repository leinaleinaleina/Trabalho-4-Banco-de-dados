package br.edu.bd.console;

import br.edu.bd.classes.casodeuso.*;
import br.edu.bd.classes.generico.ResultadoSQL;
import java.util.ArrayList;
import java.util.List;

/** Formata resultados de T4C/T4D sem executar nenhuma operacao no banco. */
public final class ConsoleDemonstracoes {
    private ConsoleDemonstracoes(){ }
    public static String formatar(List<ResultadoPerfil> perfis){
        StringBuilder texto=new StringBuilder("T4C | PERMISSOES VERIFICADAS PELO POSTGRESQL\n\n");
        for(var perfil:perfis)texto.append(formatar(perfil));
        return texto.toString();
    }
    public static String formatar(ResultadoPerfil perfil){
        StringBuilder texto=new StringBuilder("\n-- "+perfil.login()+"\n");
        for(var execucao:perfil.execucoes()) {
            texto.append('\n').append(sqlComParametros(execucao.sql(),execucao.parametros())).append(";\n");
            if(execucao.erroOriginal()!=null) {
                texto.append(execucao.erroOriginal()).append('\n');
            } else if(execucao.registros()!=null) {
                texto.append(String.format("%-8s | %-14s | %s%n","id","cnpj","razao_social"));
                texto.append("---------+----------------+----------------------------------------\n");
                for(var registro:execucao.registros())texto.append(String.format("%-8s | %-14s | %s%n",registro.id(),registro.cnpj(),registro.razaoSocial()));
                int quantidade=execucao.registros().size();
                texto.append('(').append(quantidade).append(quantidade==1?" row)\n":" rows)\n");
            }
        }
        return texto.toString();
    }
    /** Apenas exibicao: o DAO continua executando PreparedStatement com parametros. */
    private static String sqlComParametros(String sql,List<Object> parametros) {
        StringBuilder texto=new StringBuilder();
        int indice=0;
        for(int i=0;i<sql.length();i++) {
            char caractere=sql.charAt(i);
            if(caractere=='?'&&indice<parametros.size()) {
                Object valor=parametros.get(indice++);
                texto.append(valor==null?"NULL":valor instanceof Number?valor.toString():"'"+valor.toString().replace("'","''")+"'");
            } else texto.append(caractere);
        }
        return texto.toString().strip();
    }
    public static String formatar(ResultadoTransacao r){
        StringBuilder texto=new StringBuilder();
        for(var execucao:r.execucoes())texto.append(formatar(execucao));
        return texto.toString();
    }
    private static String formatar(ResultadoSQL resultado){
        StringBuilder texto=new StringBuilder("\n");
        if(resultado.sql()!=null) {
            String sql=sqlComParametros(resultado.sql(),resultado.parametros());
            texto.append(sql).append(sql.startsWith("--")?"\n":";\n");
        }
        if(resultado.erroOriginal()!=null)texto.append(resultado.erroOriginal()).append('\n');
        else if(resultado.linhas()!=null) {
            int[] larguras=new int[resultado.colunas().size()];
            for(int i=0;i<larguras.length;i++) {
                larguras[i]=resultado.colunas().get(i).length();
                for(var linha:resultado.linhas())larguras[i]=Math.max(larguras[i],valor(linha.get(i)).length());
            }
            texto.append(linha(resultado.colunas(),larguras)).append('\n');
            List<String> separadores=new ArrayList<>();
            for(int largura:larguras)separadores.add("-".repeat(largura));
            texto.append(String.join("-+-",separadores)).append('\n');
            for(var linha:resultado.linhas())texto.append(linha(linha,larguras)).append('\n');
            int quantidade=resultado.linhas().size();
            texto.append('(').append(quantidade).append(quantidade==1?" row)\n":" rows)\n");
        }
        return texto.toString();
    }
    private static String linha(List<?> valores,int[] larguras){
        List<String> campos=new ArrayList<>();
        for(int i=0;i<larguras.length;i++)campos.add(String.format("%-"+larguras[i]+"s",valor(valores.get(i))));
        return String.join(" | ",campos).stripTrailing();
    }
    private static String valor(Object valor){return valor==null?"NULL":valor.toString();}
}
