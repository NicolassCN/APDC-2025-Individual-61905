package pt.unl.fct.di.apdc.indiv.util.data;

public class RegisterData {
    public String email;
    public String username;
    public String fullName;
    public String phone;
    public String password;
    public String confirmPassword;
    public String profile;
    
    // Atributos opcionais
    public String citizenId;
    public String taxId;
    public String employer;
    public String position;
    public String address;
    public String employerTaxId;
    public String photo;

    public RegisterData() {
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getProfile() {
        return profile;
    }
}