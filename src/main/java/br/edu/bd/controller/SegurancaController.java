package br.edu.bd.controller;

import br.edu.bd.console.*;
import br.edu.bd.service.SegurancaService;

public final class SegurancaController {
    private final SegurancaService servico;
    private final ConsoleSeguranca console;
    public SegurancaController(SegurancaService servico,ConsoleSeguranca console){this.servico=servico;this.console=console;}
    public void demonstrar()throws Exception{
        while(true){
            int id=console.escolherLogin();
            if(id==0)return;
            try{
                console.resultado(servico.executar(id));
            }catch(Exception e){console.erro(e);}
        }
    }
}
