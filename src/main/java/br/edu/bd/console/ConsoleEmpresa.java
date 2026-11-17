package br.edu.bd.console;

import br.edu.bd.classes.casodeuso.Empresa;
import br.edu.bd.classes.generico.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** Formulario e impressao de empresa; sem SQL nem controle de transacao. */
public final class ConsoleEmpresa {
    private final ConsoleEntrada io;
    public ConsoleEmpresa(ConsoleEntrada io){this.io=io;}
    public void inicioCadastro(){io.titulo("T4B | CADASTRAR EMPRESA E ENDERECO COMPOSTO");}
    public Empresa lerCadastro()throws IOException{
        String cnpj;
        while(true){cnpj=io.texto("CNPJ",18).replaceAll("[./-]","");if(cnpj.matches("[0-9]{14}"))break;io.linha("O CNPJ deve conter 14 digitos.");}
        String nome=io.texto("Razao social",150);
        String tipo=io.texto("Tipo de logradouro (Rua, Avenida...)",30);
        String logradouro=io.texto("Logradouro",120);
        String numero=io.texto("Numero",20);
        String complemento=io.opcional("Complemento",80);
        String bairro=io.texto("Bairro",80);
        String cidade=io.texto("Cidade",80);
        String uf;
        while(true){uf=io.texto("UF (duas letras)",2).toUpperCase(Locale.ROOT);if(uf.matches("[A-Z]{2}"))break;io.linha("Exemplo de UF: PR.");}
        String cep;
        while(true){cep=io.texto("CEP",9).replace("-","");if(cep.matches("[0-9]{8}"))break;io.linha("O CEP deve conter oito digitos.");}
        List<Email> emails=separar(io.opcional("E-mails separados por ;",1000)).stream().map(Email::new).toList();
        List<Telefone> telefones=separar(io.opcional("Telefones como 55 41 999990001, separados por ;",1000)).stream().map(texto->{
            String[] partes=texto.split("\\s+");
            if(partes.length!=3)throw new IllegalArgumentException("Telefone: use DDI DDD NUMERO, por exemplo 55 41 999990001.");
            return new Telefone(partes[0],partes[1],partes[2]);
        }).toList();
        return new Empresa(cnpj,nome,new Endereco(tipo,logradouro,numero,complemento.isEmpty()?null:complemento,bairro,cidade,uf,cep),emails,telefones);
    }
    public boolean confirmarCadastro(Empresa empresa)throws IOException{
        Endereco a=empresa.endereco();
        io.linha("\nEmpresa: "+empresa.nome()+" | CNPJ: "+empresa.cnpj());
        io.linha("Endereco: "+a.tipo()+" "+a.logradouro()+", "+a.numero()+" | "+a.cidade()+"/"+a.uf());
        return io.confirmar("Cadastrar empresa no banco?");
    }
    public void cadastroConcluido(long id){
        io.linha("INSERT confirmado. Empresa ID "+id+"; endereco do tipo financeiro.endereco_completo.");
        io.linha("Conferencia com SELECT:");
    }
    public void listar(List<Empresa> empresas){
        io.titulo("T4B | SELECT DE EMPRESAS E ENDERECOS");
        empresas.forEach(this::exibir);
        io.linha("Empresas consultadas: "+empresas.size()+". Enderecos lidos do tipo composto nativo.");
    }
    public void exibir(Empresa empresa){
        Endereco a=empresa.endereco();
        String endereco=a.tipo()+" "+a.logradouro()+", "+a.numero()+(a.complemento()==null?"":" / "+a.complemento())+" - "+a.bairro()+" - "+a.cidade()+"/"+a.uf()+" - CEP "+a.cep();
        String emails=empresa.emails().stream().map(Email::valor).collect(Collectors.joining("; "));
        String telefones=empresa.telefones().stream().map(t->"+"+t.ddi()+" ("+t.ddd()+") "+t.numero()).collect(Collectors.joining("; "));
        io.linha("\n["+empresa.id()+"] "+empresa.nome()+" | CNPJ "+empresa.cnpj());
        io.linha("    Endereco: "+endereco);
        io.linha("    E-mails: "+(emails.isEmpty()?"-":emails));
        io.linha("    Fones:   "+(telefones.isEmpty()?"-":telefones));
    }
    private static List<String> separar(String texto){return Arrays.stream(texto.split(";")).map(String::trim).filter(s->!s.isEmpty()).toList();}
}
