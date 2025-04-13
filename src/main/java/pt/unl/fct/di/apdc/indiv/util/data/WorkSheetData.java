package pt.unl.fct.di.apdc.indiv.util.data;

public class WorkSheetData {
    public String referenciaObra;
    public String descricao;
    public String tipoAlvo;
    public String estadoAdjudicacao;
    public String dataAdjudicacao;
    public String dataInicioPrevista;
    public String dataConclusaoPrevista;
    public String contaEntidade;
    public String entidadeAdjudicacao;
    public String nifEmpresa;
    public String estadoObra;
    public String observacoes;

    public WorkSheetData() {
        // Construtor vazio necessário para o GSON
    }

    public WorkSheetData(String referenciaObra, String descricao, String tipoAlvo, String estadoAdjudicacao) {
        this.referenciaObra = referenciaObra;
        this.descricao = descricao;
        this.tipoAlvo = tipoAlvo;
        this.estadoAdjudicacao = estadoAdjudicacao;
    }
}