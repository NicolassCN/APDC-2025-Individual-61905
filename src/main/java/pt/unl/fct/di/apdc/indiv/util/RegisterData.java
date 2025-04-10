package pt.unl.fct.di.apdc.indiv.util;

public class RegisterData {
    private User user;
    private String confirmPassword;

    public RegisterData() {
    }

    public RegisterData(User user, String confirmPassword) {
        this.user = user;
        this.confirmPassword = confirmPassword;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
} 