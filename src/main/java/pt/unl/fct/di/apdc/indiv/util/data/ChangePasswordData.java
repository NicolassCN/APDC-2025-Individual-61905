package pt.unl.fct.di.apdc.indiv.util.data;

public class ChangePasswordData {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public ChangePasswordData() {
    }

    public ChangePasswordData(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

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