package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public class ChangeStateData {
    private static final Set<String> VALID_STATES = Set.of("ATIVADA", "DESATIVADA");
    
    private String targetUser;
    private String newState;
    private String requestedBy;
    private Instant requestTime;
    private String reason;
    private boolean forceUpdate;

    public ChangeStateData() {
        this.requestTime = Instant.now();
        this.forceUpdate = false;
    }

    public ChangeStateData(String targetUser, String newState, String requestedBy, String reason) {
        this();
        this.targetUser = Objects.requireNonNull(targetUser, "Target user cannot be null");
        this.newState = Objects.requireNonNull(newState, "New state cannot be null").toUpperCase();
        this.requestedBy = Objects.requireNonNull(requestedBy, "Requester cannot be null");
        this.reason = reason;
    }

    public boolean isValid() {
        return hasValidTargetUser() && 
               hasValidNewState() && 
               hasValidRequester();
    }

    private boolean hasValidTargetUser() {
        return targetUser != null && !targetUser.isBlank();
    }

    private boolean hasValidNewState() {
        return newState != null && 
               !newState.isBlank() && 
               VALID_STATES.contains(newState.toUpperCase());
    }

    private boolean hasValidRequester() {
        return requestedBy != null && !requestedBy.isBlank();
    }

    // Getters
    public String getTargetUser() { return targetUser; }
    public String getNewState() { return newState; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestTime() { return requestTime; }
    public String getReason() { return reason; }
    public boolean isForceUpdate() { return forceUpdate; }
    
    // Setters with validation
    public void setTargetUser(String targetUser) {
        this.targetUser = Objects.requireNonNull(targetUser, "Target user cannot be null");
    }
    
    public void setNewState(String newState) {
        this.newState = Objects.requireNonNull(newState, "New state cannot be null").toUpperCase();
        if (!VALID_STATES.contains(this.newState)) {
            throw new IllegalArgumentException("Invalid state: " + newState);
        }
    }
    
    public void setRequestedBy(String requestedBy) {
        this.requestedBy = Objects.requireNonNull(requestedBy, "Requester cannot be null");
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    @Override
    public String toString() {
        return String.format("StateChange[target=%s, newState=%s, by=%s, time=%s]",
            targetUser, newState, requestedBy, requestTime);
    }
}
