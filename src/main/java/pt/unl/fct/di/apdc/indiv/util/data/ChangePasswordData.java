package pt.unl.fct.di.apdc.indiv.util.data;

public class ChangePasswordData {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public ChangePasswordData() {}

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }
}