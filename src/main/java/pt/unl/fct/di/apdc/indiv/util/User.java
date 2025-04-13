package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class User {
    // Required fields
    private String email;
    private String username;
    private String fullName;
    private String phone;
    private String password;
    private String confirmPassword;
    private String profile;
    private String citizenCard;
    private String role;
    private String nif;
    private String employer;
    private String jobTitle;
    private String address;
    private String employernif;
    private String accountState;
    private String photo;

    public User() {
        // Default values
        this.role = "ENDUSER";
        this.accountState = "INACTIVE";
    }

    public static User fromEntity(Entity entity) {
        User user = new User();
        user.email = entity.getString("email");
        user.username = entity.getKey().getName();
        user.fullName = entity.getString("full_name");
        user.phone = entity.getString("phone");
        user.password = entity.getString("password");
        user.profile = entity.getString("profile");
        user.citizenCard = entity.getString("citizen_card");
        user.role = entity.getString("role");
        user.nif = entity.getString("tax_id");
        user.employer = entity.getString("employer");
        user.jobTitle = entity.getString("job_title");
        user.address = entity.getString("address");
        user.employernif = entity.getString("employer_tax_id");
        user.accountState = entity.getString("account_state");
        user.photo = entity.getString("photo");
        return user;
    }

    public Entity toEntity(Key userKey) {
        return Entity.newBuilder(userKey)
                .set("email", email != null ? email : "")
                .set("full_name", fullName != null ? fullName : "")
                .set("phone", phone != null ? phone : "")
                .set("password", password)
                .set("profile", profile != null ? profile : "private")
                .set("citizen_card", citizenCard != null ? citizenCard : "NOT DEFINED")
                .set("role", role)
                .set("tax_id", nif != null ? nif : "NOT DEFINED")
                .set("employer", employer != null ? employer : "NOT DEFINED")
                .set("job_title", jobTitle != null ? jobTitle : "NOT DEFINED")
                .set("address", address != null ? address : "NOT DEFINED")
                .set("employer_tax_id", employernif != null ? employernif : "NOT DEFINED")
                .set("account_state", accountState)
                .set("photo", photo != null ? photo : "NOT DEFINED")
                .build();
    }

    public boolean isActive() {
        return "ACTIVE".equals(accountState);
    }

    public boolean isPublicProfile() {
        return "public".equals(profile);
    }

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean isBackOffice() {
        return "BACKOFFICE".equals(role);
    }

    public boolean isEndUser() {
        return "ENDUSER".equals(role);
    }

    public boolean isPartner() {
        return "PARTNER".equals(role);
    }

    // Getters and Setters
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
    
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    
    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }
    
    public String getCitizenCard() { return citizenCard; }
    public void setCitizenCard(String citizenCard) { this.citizenCard = citizenCard; }
    
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
    
    public String getEmployerNif() { return employernif; }
    public void setEmployerNif(String employernif) { this.employernif = employernif; }
    
    public String getAccountState() { return accountState; }
    public void setAccountState(String accountState) { this.accountState = accountState; }
    
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
} 