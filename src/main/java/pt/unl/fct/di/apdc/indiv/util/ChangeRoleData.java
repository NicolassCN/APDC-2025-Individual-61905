package pt.unl.fct.di.apdc.indiv.util;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public class ChangeRoleData {
    private static final Set<String> VALID_ROLES = Set.of(
        "ENDUSER",
        "BACKOFFICE",
        "ADMIN",
        "PARTNER"
    );

    private String targetUser;
    private String newRole;
    private String requestedBy;
    private Instant requestTime;
    private String reason;
    private boolean forceUpdate;

    public ChangeRoleData() {
        this.requestTime = Instant.now();
        this.forceUpdate = false;
    }

    public ChangeRoleData(String targetUser, String newRole, String requestedBy, String reason) {
        this();
        this.targetUser = Objects.requireNonNull(targetUser, "Target user cannot be null");
        this.newRole = Objects.requireNonNull(newRole, "New role cannot be null").toUpperCase();
        this.requestedBy = Objects.requireNonNull(requestedBy, "Requester cannot be null");
        this.reason = reason;
    }

    public boolean isValid() {
        return hasValidTargetUser() && 
               hasValidNewRole() && 
               hasValidRequester();
    }

    private boolean hasValidTargetUser() {
        return targetUser != null && !targetUser.isBlank();
    }

    private boolean hasValidNewRole() {
        return newRole != null && 
               !newRole.isBlank() && 
               VALID_ROLES.contains(newRole.toUpperCase());
    }

    private boolean hasValidRequester() {
        return requestedBy != null && !requestedBy.isBlank();
    }

    // Getters
    public String getTargetUser() { return targetUser; }
    public String getNewRole() { return newRole; }
    public String getRequestedBy() { return requestedBy; }
    public Instant getRequestTime() { return requestTime; }
    public String getReason() { return reason; }
    public boolean isForceUpdate() { return forceUpdate; }
    
    // Setters
    public void setTargetUser(String targetUser) {
        this.targetUser = Objects.requireNonNull(targetUser, "Target user cannot be null");
    }
    
    public void setNewRole(String newRole) {
        this.newRole = Objects.requireNonNull(newRole, "New role cannot be null").toUpperCase();
        if (!VALID_ROLES.contains(this.newRole)) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
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
        return String.format("RoleChange[target=%s, newRole=%s, by=%s, time=%s]",
            targetUser, newRole, requestedBy, requestTime);
    }
}