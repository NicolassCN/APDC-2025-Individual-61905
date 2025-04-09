package pt.unl.fct.di.apdc.indiv.util;

public class ChangeAccountStateData {
    public String targetUsername;
    public String newState;

    public ChangeAccountStateData() {
        // Default constructor for GSON
    }

    public ChangeAccountStateData(String targetUsername, String newState) {
        this.targetUsername = targetUsername;
        this.newState = newState;
    }
} 