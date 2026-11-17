"""Gera a documentacao. Opcional: Python + reportlab; nao participa do sistema Java."""
from pathlib import Path
import textwrap
from reportlab.pdfgen import canvas
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.graphics.shapes import Drawing, Rect, Line, String, Polygon
from reportlab.graphics import renderSVG
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/pdf'
OUT.mkdir(parents=True, exist_ok=True)
NAVY = colors.HexColor('#17334D')
TEAL = colors.HexColor('#087F8C')
GRAY = colors.HexColor('#526474')
LIGHT = colors.HexColor('#EDF4F7')
styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name='TitleT4', fontName='Helvetica-Bold', fontSize=29, leading=34, textColor=NAVY, spaceAfter=18))
styles.add(ParagraphStyle(name='HeadT4', fontName='Helvetica-Bold', fontSize=18, leading=23, textColor=NAVY, spaceAfter=14))
styles.add(ParagraphStyle(name='SubT4', fontName='Helvetica-Bold', fontSize=11, leading=15, textColor=TEAL, spaceBefore=12, spaceAfter=6))
styles.add(ParagraphStyle(name='BodyT4', fontName='Helvetica', fontSize=10, leading=15, spaceAfter=10, textColor=NAVY))
styles.add(ParagraphStyle(name='SmallT4', fontName='Helvetica', fontSize=8.7, leading=12, textColor=GRAY, spaceAfter=8))
styles.add(ParagraphStyle(name='CellT4', fontName='Helvetica', fontSize=8.4, leading=11, textColor=NAVY))
styles.add(ParagraphStyle(name='CodeT4', fontName='Courier', fontSize=7.5, leading=10, textColor=NAVY, leftIndent=7, spaceAfter=10))
story = []
def p(text, style='BodyT4'):
    story.append(Paragraph(text, styles[style]))
def title(text): p(text, 'HeadT4')
def sub(text): p(text, 'SubT4')
def code(text, width=99):
    lines=[]
    for line in text.strip().splitlines():
        lines.extend(textwrap.wrap(line, width, subsequent_indent='  ', replace_whitespace=False, drop_whitespace=False) or [''])
    story.append(Preformatted('\n'.join(lines), styles['CodeT4']))
def table(rows, widths):
    cells=[[Paragraph(escape(str(c)), styles['CellT4']) for c in row] for row in rows]
    t=Table(cells, colWidths=widths, hAlign='LEFT', repeatRows=1)
    t.setStyle(TableStyle([
        ('BACKGROUND',(0,0),(-1,0),LIGHT),('VALIGN',(0,0),(-1,-1),'TOP'),
        ('LINEBELOW',(0,0),(-1,0),0.8,TEAL),('LINEBELOW',(0,1),(-1,-1),0.35,colors.HexColor('#DCE5EB')),
        ('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),8),
        ('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),
    ]));story.append(t);story.append(Spacer(1,10))
def page(): story.append(PageBreak())

def diagram():
    import math
    d=Drawing(500,600)
    def box(x,y,w,h,name,fields):
        d.add(Rect(x,y,w,h,rx=4,ry=4,strokeColor=NAVY,strokeWidth=0.7,fillColor=colors.white))
        d.add(Rect(x,y+h-23,w,23,fillColor=NAVY,strokeColor=NAVY))
        d.add(String(x+7,y+h-15,name,fontName='Helvetica-Bold',fontSize=9,fillColor=colors.white))
        for i,line in enumerate(fields):d.add(String(x+7,y+h-37-i*10,line,fontName='Helvetica',fontSize=8,fillColor=NAVY))
    def edge(points,label=None):
        for a,b in zip(points,points[1:]):d.add(Line(*a,*b,strokeColor=TEAL,strokeWidth=1))
        a,b=points[-2:];dx=b[0]-a[0];dy=b[1]-a[1];n=math.hypot(dx,dy);dx/=n;dy/=n
        d.add(Polygon([b[0],b[1],b[0]-6*dx+2.5*dy,b[1]-6*dy-2.5*dx,b[0]-6*dx-2.5*dy,b[1]-6*dy+2.5*dx],fillColor=TEAL,strokeColor=TEAL))
        if label:d.add(String(label[0],label[1],'1:N',fontName='Helvetica',fontSize=7.5,fillColor=TEAL))
    edge([(72,525),(72,500)],(80,510))
    edge([(145,457),(151,457),(151,550),(165,550)])
    edge([(145,342),(157,342),(157,520),(165,520)])
    edge([(72,205),(72,170)],(80,185))
    edge([(145,125),(162,125),(162,490),(165,490)])
    edge([(245,475),(245,440)],(253,454))
    edge([(245,315),(245,275)],(253,292))
    edge([(245,160),(245,130)],(253,141))
    edge([(325,400),(350,365)])
    edge([(325,335),(337,335),(337,215),(350,215)])
    edge([(350,555),(342,555),(342,385),(350,385)])
    edge([(422,435),(422,405)],(430,417))
    edge([(350,50),(325,50)],(328,59))
    box(0,525,145,60,'UF',['PK id','UK sigla : char(2)'])
    box(0,415,145,85,'CIDADE',['PK id','nome : varchar(80)','FK uf_id','UK nome + uf_id'])
    box(0,310,145,65,'BAIRRO',['PK id','UK nome : varchar(80)'])
    box(0,205,145,65,'TIPO_LOGRADOURO',['PK id','UK nome : varchar(30)'])
    box(0,80,145,90,'LOGRADOURO',['PK id','nome : varchar(120)','FK tipo_logradouro_id','UK nome + tipo_logradouro_id'])
    box(165,475,160,110,'ENDERECO',['PK id','cep : varchar(8)','FK bairro_id','FK cidade_id','FK logradouro_id'])
    box(165,315,160,125,'EMPRESA',['PK id','UK cnpj : varchar(14)','razao_social : varchar(150)','FK endereco_id','endereco : endereco_completo','T4B: mesmos 8 campos'])
    box(165,160,160,115,'CONTA',['PK id','FK empresa_id','nome : varchar(100)','saldo_inicial : numeric(14,2)','saldo_atual : numeric(14,2)','UK empresa_id + nome'])
    box(165,0,160,130,'LANCAMENTO',['PK id','FK conta_id / FK categoria_id','data_lancamento : date','documento : varchar(60)','observacao : varchar(300)','valor : numeric(14,2)','criado_em : timestamptz','UK conta_id + documento'])
    box(350,525,145,60,'DDI',['PK id','UK codigo : varchar(3)'])
    box(350,435,145,60,'DDD',['PK id','UK codigo : varchar(3)'])
    box(350,300,145,105,'TELEFONE_EMPRESA',['PK id','FK empresa_id','FK ddi -> ddi.codigo (UK)','FK ddd -> ddd.codigo (UK)','numero : varchar(12)'])
    box(350,170,145,90,'EMAIL_EMPRESA',['PK id','FK empresa_id','email : varchar(150)','UK empresa_id + email'])
    box(350,0,145,100,'CATEGORIA',['PK id','UK descricao : varchar(100)','natureza : char(1)','CHECK R ou D'])
    d.scale(1,0.9);d.height=540
    return d

def footer(c, doc):
    c.saveState();w,h=A4;c.setStrokeColor(TEAL);c.setLineWidth(0.6);c.line(46,41,w-46,41)
    c.setFont('Helvetica',8);c.setFillColor(GRAY);c.drawString(46,28,'BANCO DE DADOS | TRABALHO T4 | POSTGRESQL E JAVA')
    c.drawRightString(w-46,28,str(doc.page));c.restoreState()

p('BANCO DE DADOS / 2026', 'SubT4');story.append(Spacer(1,25))
p('Controle de Despesas<br/>e Receitas', 'TitleT4')
p('Trabalho T4A, T4B, T4C e T4D', 'HeadT4')
p('<b>Moises S</b><br/><b>Thaynara Nascimento de Jesus</b>')
p('Estudo de caso 1. Implementacao em Java com JDBC, menu numerado no console e PostgreSQL. Modelo unico com endereco composto nativo em Empresa.')
sub('Escopo da entrega')
table([['Parte','Implementacao'],['T4A','14 tabelas fisicas do modelo, consulta de extrato e registro de receitas/despesas.'],['T4B','Tipo endereco_completo criado com CREATE TYPE AS; INSERT e SELECT via JDBC.'],['T4C','Tres logins reais: administrador, leitor e operador que insere sem excluir.'],['T4D','INSERT + UPDATE com COMMIT, ROLLBACK intencional e interrupcao abrupta de uma JVM auxiliar.']],[65,430])
sub('Dados e ambiente')
p('Carga inicial ficticia: duas empresas, duas contas por empresa e cinco lancamentos por conta. Cada conta parte de R$ 1.150,00, antes das demonstracoes adicionais. Valores monetarios usam NUMERIC e datas usam DATE.')
p('Banco: <b>bd_t4_individual</b>. Schema: <b>financeiro</b>. A instalacao de referencia usa PostgreSQL 18 e JDK 21, com compilacao para Java 17. O novo projeto e independente dos arquivos anteriores em MySQL.', 'SmallT4')
page()

title('01 / Modelo E-R unico')
p('As 14 tabelas existem no SGBD. Setas apontam do pai ao filho (1:N); cada filho exige um pai. DDI/DDD sao referenciados por codigo UNIQUE; as demais FKs usam id.')
desenho=diagram();renderSVG.drawToFile(desenho,str(ROOT/'docs/modelo-er.svg'));story.append(desenho);story.append(Spacer(1,8))
p('<b>Legenda:</b> PK = chave primaria; UK = chave candidata UNIQUE; FK = chave estrangeira. 1 : 0..N significa que cada filho tem exatamente um pai e o pai pode ter zero ou varios filhos.', 'SmallT4')
p('Empresa.endereco conserva o tipo composto original de oito campos e endereco_id liga a Empresa ao cadastro Endereco. Nomes preservados: Conta_Bancaria = conta; TipoTransacao = categoria; Transacao = lancamento; FoneEmpresa = telefone_empresa; EmailEmpresa = email_empresa.', 'SmallT4')
page()

title('02 / Normalizacao e dicionario')
p('As tabelas separam empresas, contatos, localizacao e classificacao financeira. Lancamento referencia Conta e Categoria, sem repetir o nome da empresa ou a descricao/natureza da categoria.')
p('A extensao T4B conserva o endereco_completo de oito campos na Empresa, junto da referencia endereco_id. Os triggers vinculam os cadastros do MER na mesma transacao. Essa representacao composta e explicitada como extensao, sem alegar que ela seja escalar na 1FN classica.')
table([['Relacao','Atributos principais e restricoes'],
['empresa','id PK; cnpj UNIQUE; razao_social; endereco endereco_completo; endereco_id FK.'],
['email_empresa','id PK; empresa_id FK; email. UNIQUE(empresa_id,email).'],
['telefone_empresa','id PK; empresa_id FK; ddi/ddd FKs para codigo UNIQUE; numero. UNIQUE(empresa_id,ddi,ddd,numero).'],
['conta','id PK; empresa_id FK; nome; saldo_inicial/saldo_atual NUMERIC(14,2). UNIQUE(empresa_id,nome).'],
['categoria','id PK; descricao UNIQUE; natureza CHAR(1), R ou D.'],
['lancamento','id PK; conta_id/categoria_id FKs; data DATE; documento; observacao; valor NUMERIC > 0; criado_em. UNIQUE(conta_id,documento).'],
['uf','id PK; sigla CHAR(2) UNIQUE; duas letras maiusculas.'],
['cidade','id PK; nome VARCHAR(80); uf_id FK. UNIQUE(nome,uf_id).'],
['bairro','id PK; nome VARCHAR(80) UNIQUE.'],
['tipo_logradouro','id PK; nome VARCHAR(30) UNIQUE.'],
['logradouro','id PK; nome VARCHAR(120); tipo_logradouro_id FK. UNIQUE(nome,tipo_logradouro_id).'],
['endereco','id PK; cep VARCHAR(8); bairro_id/cidade_id/logradouro_id FKs. UNIQUE(cep,bairro_id,cidade_id,logradouro_id).'],
['ddi / ddd','Duas tabelas: cada uma com id PK e codigo VARCHAR(3) UNIQUE; um a tres digitos para DDI, dois ou tres para DDD.']],[100,395])
sub('Dependencias funcionais e saldo')
p('IDs identificam os registros; CNPJ e os codigos UNIQUE sao chaves candidatas. O composto conserva a representacao completa solicitada; a referencia relacional e preenchida no cadastro, sem alterar seu valor.')
p('Saldo atual e um agregado materializado: saldo_inicial + receitas - despesas. INSERT e UPDATE usam a mesma transacao; o extrato calcula o saldo de cada linha com SUM OVER.')
page()

title('03 / T4B: tabela objeto-relacional')
p('O tipo e criado no catalogo do PostgreSQL e permanece com seus oito campos originais. Empresa.endereco recebe esse tipo; nao e texto, JSON nem JSONB. Complemento e opcional; os outros componentes sao obrigatorios na tabela.')
code('''CREATE TYPE financeiro.endereco_completo AS (
  tipo_logradouro varchar(30), logradouro varchar(120),
  numero varchar(20), complemento varchar(80),
  bairro varchar(80), cidade varchar(80), uf char(2), cep varchar(8)
);

-- Trecho de CREATE TABLE financeiro.empresa:
endereco financeiro.endereco_completo NOT NULL''')
table([['Campo','Tipo','Regra'],['tipo_logradouro','VARCHAR(30)','Obrigatorio e nao vazio'],['logradouro','VARCHAR(120)','Obrigatorio e nao vazio'],['numero','VARCHAR(20)','Obrigatorio; aceita S/N'],['complemento','VARCHAR(80)','Opcional (NULL)'],['bairro','VARCHAR(80)','Obrigatorio e nao vazio'],['cidade','VARCHAR(80)','Obrigatorio e nao vazio'],['uf','CHAR(2)','Duas letras maiusculas'],['cep','VARCHAR(8)','Oito digitos']],[135,125,235])
p('CREATE TYPE AS nao admite NOT NULL nos seus campos. Por isso CHECKs em Empresa verificam explicitamente IS NOT NULL, textos nao vazios e formatos de UF/CEP. O SQL completo esta em sql/01_estrutura.sql.', 'SmallT4')
sub('T4B(b): SQL executado por PreparedStatement')
code('''INSERT INTO financeiro.empresa(cnpj,razao_social,endereco)
VALUES (?, ?, ROW(?,?,?,?,?,?,?,?)::financeiro.endereco_completo)
RETURNING id;

SELECT id, cnpj, razao_social, (endereco).logradouro,
       (endereco).numero, (endereco).cidade, (endereco).uf
FROM financeiro.empresa ORDER BY id;''')
p('EmpresaService controla a transacao do cadastro completo. EmpresaDAO recebe a mesma conexao e parametriza os SQLs do endereco e dos contatos. Somente o servico faz COMMIT ou ROLLBACK; o DAO nao abre nem fecha a conexao. A consulta sempre le o banco.')
page()

title('04 / T4C: criacao dos logins')
p('Na opcao 5, escolha ID 1 (administrador), ID 2 (leitor) ou ID 3 (operador). Cada escolha autentica uma conexao com o login real correspondente. Nenhum deles e superusuario nem pode criar bancos ou outros usuarios.')
sql=(ROOT/'sql/03_seguranca.sql').read_text(encoding='utf-8')
partes=sql.split('-- @comando')
code(partes[0])
sub('Senhas e isolamento')
p('PrepararBanco gera uma senha aleatoria para cada login e a configura com ALTER ROLE ... PASSWORD, sem incluir senhas no codigo ou no PDF. As credenciais de demonstracao ficam em .local/perfis.properties, em texto local e ignoradas pelo Git. A senha administrativa digitada nao e persistida.')
p('O script aplica permissoes no banco bd_t4_individual e no schema financeiro. O nome administrador significa acesso aos dados do projeto, nao superusuario do servidor. Bancos e logins de outros trabalhos nao fazem parte da demonstracao.')
table([['Login','SELECT','INSERT','UPDATE / DELETE'],['bdt4_ind_admin','Todas as tabelas','Todas as tabelas','Todas as tabelas'],['bdt4_ind_leitor','Todas as tabelas','Negado','Negado'],['bdt4_ind_operador','Todas as tabelas','Somente Empresa','Negado']],[130,100,115,150])
p('Na preparacao manual pelo pgAdmin, execute os mesmos comandos e defina senhas individuais para os logins. A preparacao Java automatiza essa etapa e verifica conflitos antes de alterar qualquer login preexistente.', 'SmallT4')
page()

title('05 / T4C: comandos de permissoes')
p('Conteudo de sql/03_seguranca.sql apos a criacao dos logins. As permissoes sao efetivamente verificadas pelo PostgreSQL. A opcao 5 do menu executa os testes com os tres logins reais.')
code('\n'.join(partes[1:]).replace('-- @comando',''))
sub('Demonstracao')
p('O console mostra os SQLs, os resultados e a mensagem original de erro do driver. ID 1: INSERT/UPDATE/DELETE usam RETURNING para mostrar a linha inserida, o nome alterado e a linha excluida; SELECT final retorna zero linhas. ID 2: SELECT e erro no INSERT. ID 3: INSERT, erro no DELETE e SELECT da linha ainda presente. As negativas reais tem SQLSTATE 42501.')
p('Cada negativa e isolada por SAVEPOINT; a transacao termina com ROLLBACK. Os cadastros existentes permanecem iguais e o menu de IDs reaparece. Digite 0 para voltar ao menu principal. Sequencias podem avancar mesmo com rollback; lacunas nos IDs sao normais.', 'SmallT4')
page()

title('06 / T4D: transacoes e evidencias')
p('FinanceiroService controla a transacao: INSERT em Lancamento e UPDATE em Conta usam a mesma conexao. Os DAOs executam SQL, sem commit, rollback, setAutoCommit ou fechamento da Connection. O servico tambem calcula o sinal da receita/despesa.')
code('''// FinanceiroService.executarTransacao:
try (Connection c = banco.abrir()) {
  c.setAutoCommit(false);
  try {
    modificar(c, lancamento, registro); // INSERT + UPDATE com RETURNING
    controlar(c, confirmar, registro); // COMMIT ou ROLLBACK
  } catch (Exception e) {
    c.rollback(); throw e; // codigo completo preserva falhas de rollback
  }
}''')
table([['Cenario','Lancamento','Saldo final'],['a) COMMIT','Um registro confirmado','B + 10,00 para uma receita'],['b) Interrupcao abrupta','Nenhum registro confirmado','B preservado'],['c) ROLLBACK intencional','Nenhum registro confirmado','B preservado']],[145,185,165])
p('B e o saldo antes de cada cenario. O console mostra os SQLs, os resultados de RETURNING e os SELECTs por outra conexao. O documento e exclusivo e o valor e R$ 10,00; despesas subtraem. Erros de SQL exibem a excecao original. COMMIT e ROLLBACK normais nao produzem erro.')
sub('Interrupcao real, sem fechamento normal')
code('''// ProcessoInterrompido.main, em uma JVM auxiliar:
Connection c = new FinanceiroService(conexoes)
    .iniciarIncompleta(lancamento, passos::add);
enviar(passos); // transmite os resultados reais para a JVM principal
Runtime.getRuntime().halt(23);
// Nao ha commit(), rollback(), close() ou finally neste processo.''')
p('A JVM principal recebe os resultados dos dois comandos e o codigo real de saida 23. Apos a perda da conexao, o PostgreSQL desfaz a transacao aberta. Os SELECTs finais exibem a ausencia do documento e o saldo preservado, sem inventar uma excecao SQL para representar a interrupcao.')
sub('Verificacao realizada')
p('Os testes conferem COMMIT de receita/despesa, interrupcao real, ROLLBACK, erros originais e RETURNING de valores alterados pelo servidor. Falhas de UPDATE desfazem INSERTs anteriores. Relatorios em docs/verificacao-t4d-sql.txt, verificacao-console.txt e verificacao-services.txt. Escritas de teste usam somente a base temporaria.')
page()

title('07 / Execucao e apresentacao')
p('<b>1.</b> No IntelliJ, abra T4-Individual/pom.xml como projeto Maven e escolha JDK 17+.<br/><b>2.</b> Execute Aplicacao.main e digite as opcoes no console Run. A preparacao inicial tambem e feita pelo console.<br/><b>3.</b> Opcao 9: consultar todas as contas. Opcao 2: registrar receita ou despesa.<br/><b>4.</b> Opcao 3: cadastrar empresa com endereco composto. Opcao 4: consultar empresas.<br/><b>5.</b> Opcao 5: escolher IDs 1, 2 e 3 para demonstrar T4C; 0 retorna ao menu. Opcoes 6, 7 e 8: COMMIT, interrupcao abrupta e ROLLBACK.<br/><b>6.</b> Opcao 0: sair. Nas listas, Enter aceita o item 1; :voltar cancela um cadastro.')
table([['Requisito','Onde encontrar'],['T4A: modelo / estrutura','docs/modelo-er.svg; sql/01_estrutura.sql e 06_tabelas_mer.sql'],['T4A: consultas e registro','FinanceiroController -> FinanceiroService -> ContaDAO / LancamentoDAO'],['T4B(a): tipo e coluna','sql/01_estrutura.sql: CREATE TYPE e CREATE TABLE empresa'],['T4B(b): JDBC INSERT / SELECT','EmpresaController -> EmpresaService -> EmpresaDAO'],['T4C: logins e permissoes','SegurancaController -> SegurancaService; sql/03_seguranca.sql'],['T4D(a,c): commit / rollback','FinanceiroController -> TransacaoService -> FinanceiroService'],['T4D(b): encerramento abrupto','demonstracao/ProcessoInterrompido; iniciarIncompleta']],[160,335])
sub('Pacotes Java')
p('classes/casodeuso e classes/generico guardam os modelos. console le/imprime; controller conduz a interacao; service aplica regras e controla transacoes. DAO contem somente EmpresaDAO, ContaDAO, CategoriaDAO e LancamentoDAO. infra guarda ConnectionFactory, Configuracao e PrepararBanco. Verificacoes automaticas ficam em src/test/java.', 'SmallT4')
sub('No pgAdmin')
p('Na instalacao de referencia, servidor 127.0.0.1, porta <b>6666</b>, banco <b>bd_t4_individual</b>, schema <b>financeiro</b>. Abra Databases &gt; bd_t4_individual &gt; Schemas &gt; financeiro &gt; Tables. O tipo composto aparece em Types. Em outras instalacoes, ajuste host e porta na preparacao.')
sub('Referencias tecnicas')
for label,url in [('PostgreSQL - tipos compostos','https://www.postgresql.org/docs/current/rowtypes.html'),('PostgreSQL - GRANT','https://www.postgresql.org/docs/current/sql-grant.html'),('PostgreSQL - transacoes','https://www.postgresql.org/docs/current/tutorial-transactions.html'),('Driver JDBC PostgreSQL','https://jdbc.postgresql.org/download/')]:
    p(f'<link href="{url}" color="#087F8C">{label}</link>', 'SmallT4')
p('A documentacao tecnica fundamenta os recursos implementados. A avaliacao academica segue os criterios do enunciado e do professor. O arquivo docs/ROTEIRO.md traz a sequencia completa para demonstrar o codigo e o banco.', 'SmallT4')

doc=SimpleDocTemplate(str(OUT/'Trabalho_T4.pdf'),pagesize=A4,rightMargin=46,leftMargin=46,topMargin=45,bottomMargin=57,
                      title='Trabalho T4 - Controle de Despesas e Receitas',author='Moises S e Thaynara Nascimento de Jesus')
doc.build(story,onFirstPage=footer,onLaterPages=footer)
print('PDF e modelo SVG gerados.')
