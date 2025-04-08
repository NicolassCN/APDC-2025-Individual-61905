package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class User {
    // Campos obrigatórios
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String password;
    private String profile; // "public" ou "private"
    
    // Campos opcionais
    private String citizenCardNumber;
    private String role;
    private String nif;
    private String employer;
    private String jobTitle;
    private String address;
    private String employerNif;
    private String accountState;
    private String photoUrl; // URL para foto em JPEG

    public User() {
        // Valores padrão
        this.role = "enduser";
        this.accountState = "DESATIVADA";
    }

    public static User fromEntity(Entity entity) {
        User user = new User();
        user.email = entity.getString("email");
        user.username = entity.getString("username");
        user.fullName = entity.getString("fullName");
        user.phone = entity.getString("phone");
        user.password = entity.getString("password");
        user.profile = entity.getString("profile");
        user.citizenCardNumber = entity.contains("citizenCardNumber") ? entity.getString("citizenCardNumber") : null;
        user.role = entity.getString("role");
        user.nif = entity.contains("nif") ? entity.getString("nif") : null;
        user.employer = entity.contains("employer") ? entity.getString("employer") : null;
        user.jobTitle = entity.contains("jobTitle") ? entity.getString("jobTitle") : null;
        user.address = entity.contains("address") ? entity.getString("address") : null;
        user.employerNif = entity.contains("employerNif") ? entity.getString("employerNif") : null;
        user.accountState = entity.getString("accountState");
        user.photoUrl = entity.contains("photoUrl") ? entity.getString("photoUrl") : null;
        return user;
    }

    public Entity toEntity(Key key) {
        Entity.Builder builder = Entity.newBuilder(key)
                .set("email", email)
                .set("username", username)
                .set("fullName", fullName)
                .set("phone", phone)
                .set("password", password)
                .set("profile", profile)
                .set("role", role)
                .set("accountState", accountState);

        // Adiciona campos opcionais apenas se não forem nulos
        if (citizenCardNumber != null) builder.set("citizenCardNumber", citizenCardNumber);
        if (nif != null) builder.set("nif", nif);
        if (employer != null) builder.set("employer", employer);
        if (jobTitle != null) builder.set("jobTitle", jobTitle);
        if (address != null) builder.set("address", address);
        if (employerNif != null) builder.set("employerNif", employerNif);
        if (photoUrl != null) builder.set("photoUrl", photoUrl);

        return builder.build();
    }

    public boolean isActive() {
        return accountState.equals("ATIVADA");
    }
    public boolean isPublicProfile() {
        return profile.equals("public");
    }
    public boolean isPrivateProfile() {
        return profile.equals("private");
    }
    public boolean isAdmin() {
        return role.equals("ADMIN");
    }
    public boolean isEndUser() {
        return role.equals("enduser");
    }
    public boolean isEmployer() {
        return role.equals("employer");
    }
    public boolean isEmployee() {
        return role.equals("employee");
    }
    public boolean isRoot() {
        return role.equals("root");
    }

    // Getters e Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    
    public String getCitizenCardNumber() { return citizenCardNumber; }
    public void setCitizenCardNumber(String citizenCardNumber) { this.citizenCardNumber = citizenCardNumber; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    
    public String getEmployer() { return employer; }
    public void setEmployer(String employer) { this.employer = employer; }
    
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getEmployerNif() { return employerNif; }
    public void setEmployerNif(String employerNif) { this.employerNif = employerNif; }
    
    public String getAccountState() { return accountState; }
    public void setAccountState(String accountState) { this.accountState = accountState; }
    
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
} 