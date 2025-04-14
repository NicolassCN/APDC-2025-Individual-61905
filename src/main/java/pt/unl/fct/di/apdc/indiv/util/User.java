package pt.unl.fct.di.apdc.indiv.util;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;

public class User {
    public enum Role {
        USER,
        GBO,
        GS,
        SU,
        ADMIN,
        PARTNER
    }

    private String username;
    private String password;
    private String email;
    private Role role;
    private String name;
    private String phone;
    private String profilePicture;
    private String address;
    private String nif;
    private String cc;
    private boolean isActive;
    private String token;
    private String fullName;
    private String profile;
    private String accountState;

    public User() {
        this.role = Role.USER;
        this.isActive = true;
        this.profile = "public";
        this.accountState = "ACTIVE";
    }

    public User(String username, String password, String email, String name, 
                String phone, String address, String nif, String cc) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.nif = nif;
        this.cc = cc;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public void setRole(String roleStr) { this.role = Role.valueOf(roleStr.toUpperCase()); }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    
    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getFullName() { return fullName != null ? fullName : name; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getProfile() { return profile; }
    public void setProfile(String profile) { this.profile = profile; }

    public String getAccountState() { return accountState; }
    public void setAccountState(String accountState) { this.accountState = accountState; }

    public Entity toEntity(Key userKey) {
        Entity.Builder builder = Entity.newBuilder(userKey)
                .set("username", this.username)
                .set("password", this.password)
                .set("email", this.email)
                .set("role", this.role.name())
                .set("name", this.name)
                .set("phone", this.phone)
                .set("address", this.address)
                .set("nif", this.nif)
                .set("cc", this.cc)
                .set("isActive", this.isActive);

        if (this.token != null) {
            builder.set("token", this.token);
        }
        if (this.profilePicture != null) {
            builder.set("profilePicture", this.profilePicture);
        }
        if (this.fullName != null) {
            builder.set("fullName", this.fullName);
        }
        if (this.profile != null) {
            builder.set("profile", this.profile);
        }
        if (this.accountState != null) {
            builder.set("accountState", this.accountState);
        }

        return builder.build();
    }
} 