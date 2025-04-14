package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class WorkSheetData {
    private static final Logger LOG = Logger.getLogger(WorkSheetData.class.getName());
    
    private static final Set<String> VALID_ADJUDICATION_STATES = Set.of("ADJUDICADO", "NÃO ADJUDICADO");
    private static final Set<String> VALID_TARGET_TYPES = Set.of("PROPRIEDADE PÚBLICA", "PROPRIEDADE PRIVADA");
    private static final String NIF_PATTERN = "^[0-9]{9}$";
    
    private String referencia;
    private String descricao;
    private String tipoAlvo;
    private String estadoAdjudicacao;
    private long dataAdjudicacao;
    private long dataInicioPrevista;
    private long dataFimPrevista;
    private String contaEntidade;
    private String nomeEmpresa;
    private String nifEmpresa;
    private String observacoes;
    private String createdBy;
    private Instant creationTime;
    private String lastModifiedBy;
    private Instant lastModifiedTime;

    public WorkSheetData() {
        this.creationTime = Instant.now();
        this.lastModifiedTime = this.creationTime;
    }

    public boolean isValid() {
        if (!validateMandatoryFields()) {
            LOG.warning("WorkSheet validation failed: Missing mandatory fields");
            return false;
        }

        if (!validateTargetType()) {
            LOG.warning("WorkSheet validation failed: Invalid target type: " + tipoAlvo);
            return false;
        }

        if (!validateAdjudicationState()) {
            LOG.warning("WorkSheet validation failed: Invalid adjudication state: " + estadoAdjudicacao);
            return false;
        }

        if (isAdjudicated() && !validateAdjudicationDetails()) {
            LOG.warning("WorkSheet validation failed: Missing or invalid adjudication details");
            return false;
        }

        return true;
    }

    private boolean validateMandatoryFields() {
        return !isBlank(referencia) && 
               !isBlank(descricao) && 
               !isBlank(tipoAlvo) && 
               !isBlank(estadoAdjudicacao);
    }

    private boolean validateTargetType() {
        return tipoAlvo != null && 
               VALID_TARGET_TYPES.contains(tipoAlvo.toUpperCase());
    }

    private boolean validateAdjudicationState() {
        return estadoAdjudicacao != null && 
               VALID_ADJUDICATION_STATES.contains(estadoAdjudicacao.toUpperCase());
    }

    private boolean validateAdjudicationDetails() {
        if (dataAdjudicacao == 0 || dataInicioPrevista == 0 || dataFimPrevista == 0) {
            return false;
        }
        
        if (isBlank(contaEntidade) || isBlank(nomeEmpresa) || isBlank(nifEmpresa)) {
            return false;
        }
        
        if (!nifEmpresa.matches(NIF_PATTERN)) {
            return false;
        }
        
        return dataInicioPrevista < dataAdjudicacao && 
               dataFimPrevista > dataInicioPrevista;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isAdjudicated() {
        return "ADJUDICADO".equals(estadoAdjudicacao.toUpperCase());
    }

    // Getters
    public String getReferencia() { return referencia; }
    public String getDescricao() { return descricao; }
    public String getTipoAlvo() { return tipoAlvo; }
    public String getEstadoAdjudicacao() { return estadoAdjudicacao; }
    public long getDataAdjudicacao() { return dataAdjudicacao; }
    public long getDataInicioPrevista() { return dataInicioPrevista; }
    public long getDataFimPrevista() { return dataFimPrevista; }
    public String getContaEntidade() { return contaEntidade; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public String getNifEmpresa() { return nifEmpresa; }
    public String getObservacoes() { return observacoes; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreationTime() { return creationTime; }
    public String getLastModifiedBy() { return lastModifiedBy; }
    public Instant getLastModifiedTime() { return lastModifiedTime; }

    // Setters with validation
    public void setReferencia(String referencia) {
        this.referencia = Objects.requireNonNull(referencia, "Reference cannot be null");
    }

    public void setDescricao(String descricao) {
        this.descricao = Objects.requireNonNull(descricao, "Description cannot be null");
    }

    public void setTipoAlvo(String tipoAlvo) {
        this.tipoAlvo = Objects.requireNonNull(tipoAlvo, "Target type cannot be null").toUpperCase();
        if (!VALID_TARGET_TYPES.contains(this.tipoAlvo)) {
            throw new IllegalArgumentException("Invalid target type: " + tipoAlvo);
        }
    }

    public void setEstadoAdjudicacao(String estadoAdjudicacao) {
        this.estadoAdjudicacao = Objects.requireNonNull(estadoAdjudicacao, "Adjudication state cannot be null").toUpperCase();
        if (!VALID_ADJUDICATION_STATES.contains(this.estadoAdjudicacao)) {
            throw new IllegalArgumentException("Invalid adjudication state: " + estadoAdjudicacao);
        }
    }

    public void setDataAdjudicacao(long dataAdjudicacao) {
        this.dataAdjudicacao = dataAdjudicacao;
    }

    public void setDataInicioPrevista(long dataInicioPrevista) {
        this.dataInicioPrevista = dataInicioPrevista;
    }

    public void setDataFimPrevista(long dataFimPrevista) {
        this.dataFimPrevista = dataFimPrevista;
    }

    public void setContaEntidade(String contaEntidade) {
        this.contaEntidade = contaEntidade;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public void setNifEmpresa(String nifEmpresa) {
        if (nifEmpresa != null && !nifEmpresa.matches(NIF_PATTERN)) {
            throw new IllegalArgumentException("Invalid company tax ID format");
        }
        this.nifEmpresa = nifEmpresa;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public void setCreatedBy(String user) {
        this.createdBy = Objects.requireNonNull(user, "Creator cannot be null");
    }

    public void setLastModifiedBy(String user) {
        this.lastModifiedBy = Objects.requireNonNull(user, "Modifier cannot be null");
        this.lastModifiedTime = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("WorkSheet[ref=%s, type=%s, state=%s, company=%s]",
            referencia, tipoAlvo, estadoAdjudicacao, nomeEmpresa);
    }
}