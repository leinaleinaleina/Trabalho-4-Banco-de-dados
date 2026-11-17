package br.edu.bd.console;

import br.edu.bd.classes.generico.Opcao;
import java.util.List;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.*;

/** Le por linha para aceitar numeros, textos e Enter no console do IntelliJ. */
public final class ConsoleEntrada {
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final String TRACO = "-".repeat(96);
    private final Console terminal = System.console();
    private final BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    private final PrintWriter saida = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    public void linha(String texto) { saida.println(texto); }
    public void titulo(String texto) { linha("\n" + TRACO + "\n" + texto + "\n" + TRACO); }
    public Opcao escolher(String titulo, List<Opcao> itens) throws IOException {
        if (itens.isEmpty()) throw new IllegalArgumentException("Nenhum registro disponivel para " + titulo + ".");
        linha("\n" + titulo);
        for (int i=0;i<itens.size();i++) linha((i+1)+" - "+itens.get(i).nome());
        linha("0 - Voltar");
        int posicao=numero("Escolha",0,itens.size(),1);
        if(posicao==0) throw new Cancelamento();
        return itens.get(posicao-1);
    }
    public String ler(String pergunta) throws IOException {
        saida.print(pergunta); saida.flush();
        String valor = terminal == null ? entrada.readLine() : terminal.readLine();
        if (valor == null) throw new EOFException();
        valor = valor.trim();
        if (valor.equalsIgnoreCase(":voltar")) throw new Cancelamento();
        return valor;
    }
    public String texto(String pergunta, int limite) throws IOException {
        while (true) {
            String valor = ler(pergunta + ": ");
            if (!valor.isBlank() && valor.length() <= limite) return valor;
            linha("Informe de 1 a " + limite + " caracteres.");
        }
    }
    public String opcional(String pergunta, int limite) throws IOException {
        while (true) {
            String valor = ler(pergunta + " [Enter para deixar vazio]: ");
            if (valor.length() <= limite) return valor;
            linha("Use no maximo " + limite + " caracteres.");
        }
    }
    public String padrao(String pergunta, String padrao) throws IOException {
        String valor = ler(pergunta + " [" + padrao + "]: ");
        return valor.isEmpty() ? padrao : valor;
    }
    public int numero(String pergunta, int minimo, int maximo, Integer padrao) throws IOException {
        while (true) {
            String valor = ler(pergunta + (padrao == null ? "" : " [" + padrao + "]") + ": ");
            if (valor.isEmpty() && padrao != null) return padrao;
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo && numero <= maximo) return numero;
            } catch (NumberFormatException ignored) { }
            linha("Escolha um numero de " + minimo + " a " + maximo + ".");
        }
    }
    public LocalDate data() throws IOException {
        while (true) {
            String valor = padrao("Data (dd/MM/aaaa)", DATA.format(LocalDate.now()));
            try { return LocalDate.parse(valor, DATA); }
            catch (DateTimeParseException e) { linha("Data invalida. Exemplo: 17/11/2026."); }
        }
    }
    public BigDecimal valor() throws IOException {
        while (true) {
            String valor = ler("Valor positivo (ex.: 120,50; sem separador de milhar): ");
            if (valor.matches("[0-9]+([,.][0-9]{1,2})?")) {
                BigDecimal numero = new BigDecimal(valor.replace(',', '.'));
                if (numero.signum() > 0 && numero.precision() - numero.scale() <= 12) return numero;
            }
            linha("Use um valor positivo com ate 12 digitos inteiros e duas casas decimais.");
        }
    }
    public boolean confirmar(String pergunta) throws IOException {
        while (true) {
            String valor = padrao(pergunta + " (s/n)", "s");
            if (valor.equalsIgnoreCase("s")) return true;
            if (valor.equalsIgnoreCase("n")) return false;
            linha("Digite s para sim ou n para nao.");
        }
    }
    public char[] senha() throws IOException {
        if (terminal != null) {
            char[] senha = terminal.readPassword("Senha administrativa do PostgreSQL: ");
            if (senha == null) throw new EOFException();
            return senha;
        }
        linha("Neste console a senha digitada fica visivel. Ela nao sera salva pelo programa.");
        saida.print("Senha administrativa do PostgreSQL: "); saida.flush();
        String senha = entrada.readLine();
        if (senha == null) throw new EOFException();
        if (senha.equals(":voltar")) throw new Cancelamento();
        return senha.toCharArray();
    }
    public static String erro(Exception e) {
        if (e instanceof SQLException sql) {
            return switch (sql.getSQLState() == null ? "" : sql.getSQLState()) {
                case "23505" -> "Registro duplicado: CNPJ, contato ou documento da conta ja cadastrado.";
                case "23514" -> "Um campo viola as regras do banco. Confira CNPJ, endereco, contatos e valor.";
                case "23503" -> "A empresa, conta ou categoria referenciada nao existe.";
                case "42501" -> "O login nao possui permissao para esta operacao (SQLSTATE 42501).";
                default -> "SQLSTATE " + sql.getSQLState() + ": " + sql.getMessage();
            };
        }
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }
    public static final class Cancelamento extends RuntimeException { }
}
