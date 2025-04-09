package pt.unl.fct.di.apdc.indiv.util;

public class RemoveUserAccountData {
    public String targetUsername; // Can be username or email
    public boolean isEmail; // Flag to indicate if targetUsername is an email

    public RemoveUserAccountData() {
        // Default constructor for GSON
    }

    public RemoveUserAccountData(String targetUsername, boolean isEmail) {
        this.targetUsername = targetUsername;
        this.isEmail = isEmail;
    }
} 