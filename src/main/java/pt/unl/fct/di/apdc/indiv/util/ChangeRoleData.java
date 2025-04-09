package pt.unl.fct.di.apdc.indiv.util;

public class ChangeRoleData {
    public String targetUsername;
    public String newRole;

    public ChangeRoleData() {
        // Default constructor for GSON
    }

    public ChangeRoleData(String targetUsername, String newRole) {
        this.targetUsername = targetUsername;
        this.newRole = newRole;
    }
} 