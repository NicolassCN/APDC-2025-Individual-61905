package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class UpdateWorkStateData {
    private static final Logger LOG = Logger.getLogger(UpdateWorkStateData.class.getName());
    
    private static final Set<String> VALID_WORK_STATES = Set.of(
        "NÃO INICIADO",
        "EM CURSO",
        "CONCLUÍDO"
    );

    private String referencia;
    private String newWorkState;
    private String updatedBy;
    private Instant updateTime;
    private String reason;
    private String comments;
    private boolean forceUpdate;

    public UpdateWorkStateData() {
        this.updateTime = Instant.now();
        this.forceUpdate = false;
    }

    public UpdateWorkStateData(String referencia, String newWorkState, String updatedBy, String reason) {
        this();
        this.referencia = Objects.requireNonNull(referencia, "Reference cannot be null");
        this.newWorkState = Objects.requireNonNull(newWorkState, "Work state cannot be null").toUpperCase();
        this.updatedBy = Objects.requireNonNull(updatedBy, "Updater cannot be null");
        this.reason = reason;
    }

    public boolean isValid() {
        if (!validateReference()) {
            LOG.warning("UpdateWorkState validation failed: Invalid reference");
            return false;
        }

        if (!validateWorkState()) {
            LOG.warning("UpdateWorkState validation failed: Invalid work state: " + newWorkState);
            return false;
        }

        if (!validateUpdater()) {
            LOG.warning("UpdateWorkState validation failed: Invalid updater");
            return false;
        }

        return true;
    }

    private boolean validateReference() {
        return referencia != null && !referencia.isBlank();
    }

    private boolean validateWorkState() {
        return newWorkState != null && 
               !newWorkState.isBlank() && 
               VALID_WORK_STATES.contains(newWorkState.toUpperCase());
    }

    private boolean validateUpdater() {
        return updatedBy != null && !updatedBy.isBlank();
    }

    // Getters
    public String getReferencia() { return referencia; }
    public String getNewWorkState() { return newWorkState; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdateTime() { return updateTime; }
    public String getReason() { return reason; }
    public String getComments() { return comments; }
    public boolean isForceUpdate() { return forceUpdate; }

    // Setters with validation
    public void setReferencia(String referencia) {
        this.referencia = Objects.requireNonNull(referencia, "Reference cannot be null");
    }

    public void setNewWorkState(String state) {
        this.newWorkState = Objects.requireNonNull(state, "Work state cannot be null").toUpperCase();
        if (!VALID_WORK_STATES.contains(this.newWorkState)) {
            throw new IllegalArgumentException("Invalid work state: " + state);
        }
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = Objects.requireNonNull(updatedBy, "Updater cannot be null");
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    @Override
    public String toString() {
        return String.format("WorkStateUpdate[ref=%s, state=%s, by=%s, time=%s]",
            referencia, newWorkState, updatedBy, updateTime);
    }
}
